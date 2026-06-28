package com.hotel.member.controller;

import com.hotel.common.core.Result;
import com.hotel.common.idempotent.Idempotent;
import com.hotel.member.model.dto.*;
import com.hotel.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/register")
    @Idempotent(keyPrefix = "member:register", fields = {"phone"}, expireSeconds = 3600,
            deleteOnSuccess = true, concurrentStrategy = Idempotent.ConcurrentStrategy.THROW)
    public Result<MemberResponse> register(@RequestBody @Valid RegisterMemberRequest request) {
        return Result.success(memberService.register(request));
    }

    @GetMapping("/query/{memberId}")
    public Result<MemberResponse> query(@PathVariable String memberId) {
        return Result.success(memberService.queryMember(memberId));
    }

    @GetMapping("/query/phone/{phone}")
    public Result<MemberResponse> queryByPhone(@PathVariable String phone) {
        return Result.success(memberService.queryByPhone(phone));
    }

    @PostMapping("/points/earn")
    @Idempotent(keyPrefix = "member:points:earn", fields = {"memberId", "businessId"}, expireSeconds = 3600,
            deleteOnSuccess = true)
    public Result<PointsResponse> earnPoints(@RequestBody @Valid EarnPointsRequest request) {
        return Result.success(memberService.earnPoints(request));
    }

    @PostMapping("/points/deduct")
    @Idempotent(keyPrefix = "member:points:deduct", fields = {"memberId"}, expireSeconds = 3600)
    public Result<PointsResponse> deductPoints(@RequestBody Map<String, Object> body) {
        String memberId = (String) body.get("memberId");
        Long points = Long.valueOf(body.get("points").toString());
        String description = (String) body.getOrDefault("description", "积分扣减");
        return Result.success(memberService.deductPoints(memberId, points, description));
    }

    @GetMapping("/points/logs/{memberId}")
    public Result<List<Map<String, Object>>> getPointsLogs(@PathVariable String memberId,
                                                            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(memberService.getPointsLogs(memberId, limit));
    }

    @GetMapping("/levels")
    public Result<List<Map<String, Object>>> getLevels() {
        return Result.success(memberService.getLevelConfigs());
    }

    @PostMapping("/points/expire")
    public Result<Integer> processExpiredPoints() {
        return Result.success(memberService.processExpiredPoints());
    }
}
