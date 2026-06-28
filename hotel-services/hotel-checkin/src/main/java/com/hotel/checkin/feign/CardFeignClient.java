package com.hotel.checkin.feign;

import com.hotel.checkin.model.dto.CheckInRequest;
import com.hotel.checkin.model.dto.CheckInResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "card-service", fallbackFactory = CardFeignFallback.class)
public interface CardFeignClient {

    @PostMapping("/api/card/create")
    Map<String, Object> createCard(@RequestBody Map<String, Object> request);

    @PostMapping("/api/card/extend")
    Map<String, Object> extendCard(@RequestBody Map<String, Object> request);

    @PostMapping("/api/card/cancel")
    Map<String, Object> cancelCard(@RequestBody Map<String, Object> request);

    @GetMapping("/api/card/checkin/{checkinId}")
    Map<String, Object> getCardsByCheckinId(@PathVariable String checkinId);
}
