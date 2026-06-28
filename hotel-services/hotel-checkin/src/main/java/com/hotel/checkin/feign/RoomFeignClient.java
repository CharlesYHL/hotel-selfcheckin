package com.hotel.checkin.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "room-service", fallbackFactory = RoomFeignFallback.class)
public interface RoomFeignClient {

    @PostMapping("/api/room/release")
    Map<String, Object> releaseRoom(@RequestBody Map<String, Object> request);
}
