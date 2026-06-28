package com.hotel.checkin.controller;

import com.hotel.checkin.model.dto.*;
import com.hotel.checkin.service.CheckInService;
import com.hotel.common.core.Result;
import com.hotel.common.idempotent.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping("/checkin")
    @Idempotent(keyPrefix = "checkin:create", fields = {"orderId"}, expireSeconds = 3600,
            deleteOnSuccess = true, concurrentStrategy = Idempotent.ConcurrentStrategy.THROW)
    public Result<CheckInResponse> checkIn(@RequestBody @Valid CheckInRequest request) {
        return Result.success(checkInService.checkIn(request));
    }

    @PostMapping("/extend")
    @Idempotent(keyPrefix = "checkin:extend", fields = {"checkinId"}, expireSeconds = 3600)
    public Result<CheckInResponse> extendStay(@RequestBody @Valid ExtendStayRequest request) {
        return Result.success(checkInService.extendStay(request));
    }

    @PostMapping("/checkout/{checkinId}")
    @Idempotent(keyPrefix = "checkin:checkout", fields = {"checkinId"}, expireSeconds = 3600,
            deleteOnSuccess = true, concurrentStrategy = Idempotent.ConcurrentStrategy.THROW)
    public Result<CheckOutResponse> checkOut(@PathVariable String checkinId,
                                              @RequestParam(defaultValue = "1") Integer checkoutType) {
        return Result.success(checkInService.checkOut(checkinId, checkoutType));
    }

    @GetMapping("/query/order/{orderId}")
    public Result<CheckInResponse> queryByOrderId(@PathVariable String orderId) {
        return Result.success(checkInService.queryByOrderId(orderId));
    }

    @GetMapping("/query/{checkinId}")
    public Result<CheckInResponse> queryByCheckinId(@PathVariable String checkinId) {
        return Result.success(checkInService.queryByCheckinId(checkinId));
    }

    @GetMapping("/guests/{checkinId}")
    public Result<List<Map<String, Object>>> getGuests(@PathVariable String checkinId) {
        return Result.success(checkInService.getGuests(checkinId));
    }
}
