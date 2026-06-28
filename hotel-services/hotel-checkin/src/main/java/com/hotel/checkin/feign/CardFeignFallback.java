package com.hotel.checkin.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class CardFeignFallback implements FallbackFactory<CardFeignClient> {

    @Override
    public CardFeignClient create(Throwable cause) {
        log.error("制卡服务调用失败，触发降级: {}", cause.getMessage());
        return new CardFeignClient() {
            @Override
            public Map<String, Object> createCard(Map<String, Object> request) {
                log.warn("[Fallback] 制卡降级 - 创建门卡请求已记录, checkinId={}", request.get("checkinId"));
                return Collections.singletonMap("status", "FALLBACK");
            }

            @Override
            public Map<String, Object> extendCard(Map<String, Object> request) {
                log.warn("[Fallback] 制卡降级 - 延期请求已记录");
                return Collections.singletonMap("status", "FALLBACK");
            }

            @Override
            public Map<String, Object> cancelCard(Map<String, Object> request) {
                log.warn("[Fallback] 制卡降级 - 注销请求已记录");
                return Collections.singletonMap("status", "FALLBACK");
            }

            @Override
            public Map<String, Object> getCardsByCheckinId(String checkinId) {
                return Collections.emptyMap();
            }
        };
    }
}
