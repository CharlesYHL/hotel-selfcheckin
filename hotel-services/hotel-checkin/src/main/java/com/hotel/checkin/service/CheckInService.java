package com.hotel.checkin.service;

import com.hotel.checkin.feign.CardFeignClient;
import com.hotel.checkin.feign.RoomFeignClient;
import com.hotel.checkin.model.dto.*;
import com.hotel.checkin.model.entity.*;
import com.hotel.checkin.model.enums.CheckInStatus;
import com.hotel.checkin.model.enums.GuestType;
import com.hotel.checkin.model.enums.VerifyStatus;
import com.hotel.checkin.model.event.CheckInEvent;
import com.hotel.checkin.repository.*;
import com.hotel.common.core.BusinessException;
import com.hotel.common.security.IdCardData;
import com.hotel.common.security.IdCardEncryptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInMapper checkInMapper;
    private final GuestMapper guestMapper;
    private final ExtensionMapper extensionMapper;
    private final CheckOutMapper checkOutMapper;
    private final IdentityVerifyService verifyService;
    private final IdCardEncryptService encryptService;
    private final CardFeignClient cardFeignClient;
    private final RoomFeignClient roomFeignClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 办理入住：身份核验 → 创建入住记录 → 通知制卡。
     */
    @Transactional
    public CheckInResponse checkIn(CheckInRequest request) {
        // 1. 检查是否已入住
        CheckIn existing = checkInMapper.selectByOrderId(request.getOrderId());
        if (existing != null && existing.getStatus() != CheckInStatus.NO_SHOW.getCode()) {
            throw new BusinessException("该订单已办理入住");
        }

        // 2. 身份核验
        if (request.getGuests() != null && !request.getGuests().isEmpty()) {
            List<VerifyResult> results = verifyService.verifyGuests(request.getGuests());
            long failed = results.stream().filter(r -> !r.isPassed() && r.getVerifyStatus() == VerifyStatus.FAILED.getCode()).count();
            if (failed > 0) {
                throw new BusinessException("身份核验未通过，请检查入住人信息");
            }
        }

        // 3. 创建入住记录
        CheckIn checkIn = new CheckIn();
        checkIn.setCheckinId("CHK" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        checkIn.setCheckinNo("CN" + System.currentTimeMillis());
        checkIn.setOrderId(request.getOrderId());
        checkIn.setOrderNo(request.getOrderId());
        checkIn.setHotelId(request.getHotelId());
        checkIn.setMemberId(request.getMemberId());
        checkIn.setRoomId(request.getRoomId());
        checkIn.setRoomNo(request.getRoomId());
        checkIn.setRoomTypeId(request.getHotelId());
        checkIn.setRoomTypeName("标准房");
        checkIn.setCheckInTime(LocalDateTime.now());
        checkIn.setCheckOutTime(LocalDateTime.now().plusDays(1));
        checkIn.setAdults(request.getGuests() != null ? (int) request.getGuests().stream()
                .filter(g -> g.getGuestType() == GuestType.PRIMARY.getCode()).count() : 1);
        checkIn.setChildren(0);
        checkIn.setTotalAmount(BigDecimal.ZERO);
        checkIn.setPaidAmount(BigDecimal.ZERO);
        checkIn.setDueAmount(BigDecimal.ZERO);
        checkIn.setStatus(CheckInStatus.CHECKED_IN.getCode());
        checkIn.setCheckinChannel(request.getCheckinChannel() != null ? request.getCheckinChannel() : 1);
        checkIn.setVerifyStatus(VerifyStatus.PASSED.getCode());
        checkIn.setCreatedTime(LocalDateTime.now());
        checkIn.setUpdatedTime(LocalDateTime.now());
        checkInMapper.insert(checkIn);

        // 4. 保存入住人信息（PIPL 合规加密）
        if (request.getGuests() != null) {
            for (CheckInRequest.GuestInfo g : request.getGuests()) {
                IdCardData encrypted = encryptService.encrypt(g.getIdCardNo());
                Guest guest = new Guest();
                guest.setGuestId("GST" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
                guest.setCheckinId(checkIn.getCheckinId());
                guest.setHotelId(request.getHotelId());
                guest.setGuestName(g.getGuestName());
                guest.setGuestType(g.getGuestType());
                guest.setIdCardType(g.getIdCardType());
                guest.setIdCardNo(encrypted.getIdCardMasked());
                guest.setIdCardHash(encrypted.getIdCardHash());
                guest.setIdCardEncrypted(encrypted.getIdCardEncrypted());
                guest.setKeyVersion(encrypted.getKeyVersion());
                guest.setSalt(encrypted.getSalt());
                guest.setIdCardMasked(encrypted.getIdCardMasked());
                guest.setGender(encrypted.getGender());
                guest.setBirthDate(encrypted.getBirthDate());
                guest.setPhone(g.getPhone());
                guest.setVerifyStatus(VerifyStatus.PASSED.getCode());
                guest.setCreatedTime(LocalDateTime.now());
                guestMapper.insert(guest);
            }
        }

        // 5. 通知制卡服务（通过 Kafka）
        List<String> guestNames = request.getGuests() != null
                ? request.getGuests().stream().map(CheckInRequest.GuestInfo::getGuestName).collect(Collectors.toList())
                : Collections.emptyList();

        CheckInEvent event = CheckInEvent.builder()
                .checkinId(checkIn.getCheckinId())
                .checkinNo(checkIn.getCheckinNo())
                .orderId(checkIn.getOrderId())
                .orderNo(checkIn.getOrderNo())
                .hotelId(checkIn.getHotelId())
                .memberId(checkIn.getMemberId())
                .roomId(checkIn.getRoomId())
                .roomNo(checkIn.getRoomNo())
                .checkInTime(checkIn.getCheckInTime())
                .checkOutTime(checkIn.getCheckOutTime())
                .eventType("CHECKED_IN")
                .guestNames(guestNames)
                .timestamp(System.currentTimeMillis())
                .build();
        kafkaTemplate.send("checkin-events", event);

        log.info("入住办理成功: checkinId={}, orderId={}, roomId={}", checkIn.getCheckinId(), checkIn.getOrderId(), checkIn.getRoomId());

        return toResponse(checkIn);
    }

    /**
     * 续住：延长退房时间。
     */
    @Transactional
    public CheckInResponse extendStay(ExtendStayRequest request) {
        CheckIn checkIn = checkInMapper.selectById(request.getCheckinId());
        if (checkIn == null) {
            throw new BusinessException("入住记录不存在");
        }
        if (checkIn.getStatus() != CheckInStatus.CHECKED_IN.getCode()
                && checkIn.getStatus() != CheckInStatus.EXTENDED.getCode()) {
            throw new BusinessException("当前状态不允许续住");
        }

        Extension ext = new Extension();
        ext.setExtensionId("EXT" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        ext.setCheckinId(checkIn.getCheckinId());
        ext.setHotelId(checkIn.getHotelId());
        ext.setRoomId(checkIn.getRoomId());
        ext.setExtendFrom(checkIn.getCheckOutTime());
        ext.setExtendTo(checkIn.getCheckOutTime().plusDays(request.getExtendDays()));
        ext.setExtendDays(request.getExtendDays());
        ext.setNightlyRate(checkIn.getTotalAmount());
        ext.setExtendAmount(checkIn.getTotalAmount().multiply(BigDecimal.valueOf(request.getExtendDays())));
        ext.setPayStatus(1);
        ext.setCreatedTime(LocalDateTime.now());
        extensionMapper.insert(ext);

        checkIn.setCheckOutTime(ext.getExtendTo());
        checkIn.setStatus(CheckInStatus.EXTENDED.getCode());
        checkIn.setUpdatedTime(LocalDateTime.now());
        checkInMapper.updateById(checkIn);

        // 通知制卡服务延期门卡
        kafkaTemplate.send("checkin-events", CheckInEvent.builder()
                .checkinId(checkIn.getCheckinId())
                .orderId(checkIn.getOrderId())
                .roomId(checkIn.getRoomId())
                .eventType("EXTENDED")
                .checkOutTime(ext.getExtendTo())
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("续住成功: checkinId={}, extendDays={}, newCheckOut={}", checkIn.getCheckinId(), request.getExtendDays(), ext.getExtendTo());
        return toResponse(checkIn);
    }

    /**
     * 办理退房：更新状态 → 释放房间 → 注销门卡。
     */
    @Transactional
    public CheckOutResponse checkOut(String checkinId, Integer checkoutType) {
        CheckIn checkIn = checkInMapper.selectById(checkinId);
        if (checkIn == null) {
            throw new BusinessException("入住记录不存在");
        }
        if (checkIn.getStatus() == CheckInStatus.CHECKED_OUT.getCode()) {
            throw new BusinessException("该入住已退房");
        }

        // 更新入住状态
        int updated = checkInMapper.updateStatus(checkIn.getCheckinId(),
                CheckInStatus.CHECKED_OUT.getCode(), checkIn.getStatus());
        if (updated == 0) {
            throw new BusinessException("入住状态变更失败，可能已被并发操作");
        }

        LocalDateTime now = LocalDateTime.now();
        checkIn.setActualCheckoutTime(now);

        // 计算费用
        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn.getCheckInTime(), now);
        if (nights == 0) nights = 1;

        // 创建退房记录
        CheckOut checkout = new CheckOut();
        checkout.setCheckoutId("CKO" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        checkout.setCheckinId(checkIn.getCheckinId());
        checkout.setHotelId(checkIn.getHotelId());
        checkout.setRoomId(checkIn.getRoomId());
        checkout.setRoomNo(checkIn.getRoomNo());
        checkout.setCheckInTime(checkIn.getCheckInTime());
        checkout.setCheckOutTime(now);
        checkout.setNights((int) nights);
        checkout.setRoomAmount(checkIn.getTotalAmount());
        checkout.setExtraAmount(BigDecimal.ZERO);
        checkout.setTotalAmount(checkIn.getTotalAmount());
        checkout.setPaidAmount(checkIn.getPaidAmount());
        checkout.setRefundAmount(BigDecimal.ZERO);
        checkout.setDueAmount(BigDecimal.ZERO);
        checkout.setCheckoutType(checkoutType != null ? checkoutType : 1);
        checkout.setCheckoutStatus(1);
        checkout.setCardReturn(1);
        checkout.setItemCheck(1);
        checkout.setCreatedTime(now);
        checkOutMapper.insert(checkout);

        // 释放房间
        try {
            Map<String, Object> releaseReq = new HashMap<>();
            releaseReq.put("roomId", checkIn.getRoomId());
            releaseReq.put("orderId", checkIn.getOrderId());
            roomFeignClient.releaseRoom(releaseReq);
        } catch (Exception e) {
            log.error("释放房间失败，需人工处理: roomId={}, orderId={}", checkIn.getRoomId(), checkIn.getOrderId(), e);
        }

        // 通知制卡服务注销门卡
        kafkaTemplate.send("checkin-events", CheckInEvent.builder()
                .checkinId(checkIn.getCheckinId())
                .orderId(checkIn.getOrderId())
                .roomId(checkIn.getRoomId())
                .eventType("CHECKED_OUT")
                .checkOutTime(now)
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("退房成功: checkinId={}, roomId={}, nights={}", checkIn.getCheckinId(), checkIn.getRoomId(), nights);

        return CheckOutResponse.builder()
                .checkoutId(checkout.getCheckoutId())
                .checkinId(checkIn.getCheckinId())
                .roomId(checkIn.getRoomId())
                .roomNo(checkIn.getRoomNo())
                .checkInTime(checkIn.getCheckInTime())
                .checkOutTime(now)
                .nights((int) nights)
                .totalAmount(checkIn.getTotalAmount())
                .paidAmount(checkIn.getPaidAmount())
                .refundAmount(BigDecimal.ZERO)
                .dueAmount(BigDecimal.ZERO)
                .checkoutStatus(1)
                .checkoutStatusDesc("正常退房")
                .createdTime(now)
                .build();
    }

    /**
     * 查询入住信息。
     */
    public CheckInResponse queryByOrderId(String orderId) {
        CheckIn checkIn = checkInMapper.selectByOrderId(orderId);
        if (checkIn == null) {
            throw new BusinessException("未找到入住记录");
        }
        return toResponse(checkIn);
    }

    public CheckInResponse queryByCheckinId(String checkinId) {
        CheckIn checkIn = checkInMapper.selectById(checkinId);
        if (checkIn == null) {
            throw new BusinessException("入住记录不存在");
        }
        return toResponse(checkIn);
    }

    /**
     * 查询入住人列表。
     */
    public List<Map<String, Object>> getGuests(String checkinId) {
        return guestMapper.selectByCheckinId(checkinId).stream().map(g -> {
            Map<String, Object> m = new HashMap<>();
            m.put("guestId", g.getGuestId());
            m.put("guestName", g.getGuestName());
            m.put("guestType", g.getGuestType());
            m.put("idCardType", g.getIdCardType());
            m.put("idCardMasked", g.getIdCardMasked());
            m.put("gender", g.getGender());
            m.put("birthDate", g.getBirthDate());
            m.put("verifyStatus", g.getVerifyStatus());
            return m;
        }).collect(Collectors.toList());
    }

    private CheckInResponse toResponse(CheckIn c) {
        CheckInStatus status = CheckInStatus.of(c.getStatus());
        VerifyStatus vs = VerifyStatus.of(c.getVerifyStatus());
        return CheckInResponse.builder()
                .checkinId(c.getCheckinId())
                .checkinNo(c.getCheckinNo())
                .orderId(c.getOrderId())
                .roomId(c.getRoomId())
                .roomNo(c.getRoomNo())
                .hotelId(c.getHotelId())
                .memberId(c.getMemberId())
                .checkInTime(c.getCheckInTime())
                .checkOutTime(c.getCheckOutTime())
                .status(c.getStatus())
                .statusDesc(status.getDesc())
                .verifyStatus(c.getVerifyStatus())
                .verifyStatusDesc(vs.getDesc())
                .cardNo(c.getCardNo())
                .createdTime(c.getCreatedTime())
                .build();
    }
}
