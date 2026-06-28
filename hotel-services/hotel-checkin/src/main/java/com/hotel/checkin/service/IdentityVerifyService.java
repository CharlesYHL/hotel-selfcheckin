package com.hotel.checkin.service;

import com.hotel.checkin.model.dto.CheckInRequest;
import com.hotel.checkin.model.dto.VerifyResult;
import com.hotel.checkin.model.enums.VerifyStatus;
import com.hotel.common.security.IdCardEncryptService;
import com.hotel.common.security.IdCardData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 身份核验服务。调用公安接口进行身份证实名核验，支持降级。
 * 遵循 P1 缺陷12: 外部依赖容错（公安接口降级）。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IdentityVerifyService {

    private final IdCardEncryptService encryptService;

    @Value("${checkin.identity.verify-timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${checkin.identity.retry-max:3}")
    private int retryMax;

    @Value("${checkin.identity.fallback-enabled:true}")
    private boolean fallbackEnabled;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * 批量核验入住人身份。先加密存储身份证，再调用公安接口。
     */
    public List<VerifyResult> verifyGuests(List<CheckInRequest.GuestInfo> guests) {
        List<VerifyResult> results = new ArrayList<>();
        for (CheckInRequest.GuestInfo guest : guests) {
            results.add(verifySingle(guest));
        }
        return results;
    }

    /**
     * 核验单个入住人身份。带重试和降级。
     */
    public VerifyResult verifySingle(CheckInRequest.GuestInfo guest) {
        // Step 1: 加密存储身份证号（PIPL 合规）
        IdCardData encrypted = encryptService.encrypt(guest.getIdCardNo());

        // Step 2: 调用公安接口核验
        for (int i = 0; i < retryMax; i++) {
            try {
                boolean passed = callPoliceApi(guest, encrypted);
                if (passed) {
                    return VerifyResult.builder()
                            .passed(true)
                            .message("身份核验通过")
                            .verifyStatus(VerifyStatus.PASSED.getCode())
                            .build();
                }
                return VerifyResult.builder()
                        .passed(false)
                        .message("身份核验未通过")
                        .verifyStatus(VerifyStatus.FAILED.getCode())
                        .build();
            } catch (TimeoutException e) {
                log.warn("公安接口超时，重试 {}/{}: {}", i + 1, retryMax, e.getMessage());
            } catch (Exception e) {
                log.error("公安接口异常，重试 {}/{}: {}", i + 1, retryMax, e.getMessage());
            }
        }

        // Step 3: 降级策略
        if (fallbackEnabled) {
            log.warn("公安接口不可用，触发降级: 转人工审核, guestName={}", guest.getGuestName());
            return VerifyResult.builder()
                    .passed(true)
                    .message("系统降级：转人工审核")
                    .verifyStatus(VerifyStatus.MANUAL_REVIEW.getCode())
                    .build();
        }

        return VerifyResult.builder()
                .passed(false)
                .message("身份核验服务不可用")
                .verifyStatus(VerifyStatus.FAILED.getCode())
                .build();
    }

    /**
     * 模拟调用公安身份核验接口。
     * 生产环境对接公安部实名认证 API。
     */
    private boolean callPoliceApi(CheckInRequest.GuestInfo guest, IdCardData encrypted)
            throws TimeoutException, ExecutionException, InterruptedException {
        Future<Boolean> future = executor.submit(() -> {
            // 模拟公安接口核验逻辑
            // 生产环境: 调用公安部 /idcard/verify 接口
            // 传入姓名 + 身份证号，返回匹配结果
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(200, 800));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 模拟: 身份证号长度 == 18 位则通过
            return guest.getIdCardNo() != null && guest.getIdCardNo().length() == 18;
        });

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    private static class ThreadLocalRandom {
        static java.util.concurrent.ThreadLocalRandom current() {
            return java.util.concurrent.ThreadLocalRandom.current();
        }
    }
}
