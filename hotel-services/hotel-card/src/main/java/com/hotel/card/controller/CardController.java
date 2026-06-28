package com.hotel.card.controller;

import com.hotel.card.model.dto.*;
import com.hotel.card.service.CardService;
import com.hotel.common.core.Result;
import com.hotel.common.idempotent.Idempotent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/card")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping("/create")
    @Idempotent(keyPrefix = "card:create", fields = {"checkinId"}, expireSeconds = 3600,
            deleteOnSuccess = true, concurrentStrategy = Idempotent.ConcurrentStrategy.THROW)
    public Result<CardResponse> create(@RequestBody @Valid CreateCardRequest request) {
        return Result.success(cardService.createCard(request));
    }

    @PostMapping("/replace/{cardId}")
    @Idempotent(keyPrefix = "card:replace", fields = {"cardId"}, expireSeconds = 3600,
            deleteOnSuccess = true)
    public Result<CardResponse> replace(@PathVariable String cardId,
                                         @RequestParam(required = false) String operatorId) {
        return Result.success(cardService.replaceCard(cardId, operatorId));
    }

    @PostMapping("/extend/{cardId}")
    @Idempotent(keyPrefix = "card:extend", fields = {"cardId"}, expireSeconds = 3600)
    public Result<CardResponse> extend(@PathVariable String cardId,
                                        @RequestBody Map<String, Object> body) {
        String newValidToStr = (String) body.get("newValidTo");
        java.time.LocalDateTime newValidTo = java.time.LocalDateTime.parse(newValidToStr);
        return Result.success(cardService.extendCard(cardId, newValidTo));
    }

    @PostMapping("/cancel/{cardId}")
    @Idempotent(keyPrefix = "card:cancel", fields = {"cardId"}, expireSeconds = 3600,
            deleteOnSuccess = true)
    public Result<CardResponse> cancel(@PathVariable String cardId,
                                        @RequestParam(required = false) String operatorId) {
        return Result.success(cardService.cancelCard(cardId, operatorId));
    }

    @PostMapping("/open/{cardId}")
    public Result<Map<String, Object>> openDoor(@PathVariable String cardId,
                                                 @RequestParam(defaultValue = "APP") String device) {
        return Result.success(cardService.openDoor(cardId, device));
    }

    @GetMapping("/query/{cardId}")
    public Result<CardResponse> query(@PathVariable String cardId) {
        return Result.success(cardService.queryCard(cardId));
    }

    @GetMapping("/checkin/{checkinId}")
    public Result<List<CardResponse>> queryByCheckinId(@PathVariable String checkinId) {
        return Result.success(cardService.queryCardsByCheckinId(checkinId));
    }

    @GetMapping("/logs/{cardId}")
    public Result<List<Map<String, Object>>> getLogs(@PathVariable String cardId) {
        return Result.success(cardService.getCardLogs(cardId));
    }
}
