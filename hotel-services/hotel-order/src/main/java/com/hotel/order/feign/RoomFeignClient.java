package com.hotel.order.feign;

import com.hotel.common.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "room-service", url = "${feign.room.url:http://localhost:8082}")
public interface RoomFeignClient {

    @PostMapping("/api/room/assign")
    Result<Map<String, Object>> assignRoom(@RequestBody Map<String, Object> request);

    @PostMapping("/api/room/release")
    Result<Void> releaseRoom(@RequestBody Map<String, Object> request);
}
