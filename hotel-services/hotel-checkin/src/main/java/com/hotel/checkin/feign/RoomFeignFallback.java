package com.hotel.checkin.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RoomFeignFallback implements FallbackFactory<RoomFeignClient> {

    @Override
    public RoomFeignClient create(Throwable cause) {
        log.error("房态服务调用失败，触发降级: {}", cause.getMessage());
        return request -> {
            log.error("[Fallback] 释放房间降级 - roomId={}, orderId={}", request.get("roomId"), request.get("orderId"));
            throw new RuntimeException("房态服务不可用，请稍后重试");
        };
    }
}
