package com.hotel.payment.controller;

import com.hotel.common.core.Result;
import com.hotel.common.idempotent.Idempotent;
import com.hotel.payment.model.dto.*;
import com.hotel.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    @Idempotent(keyPrefix = "payment:create", fields = {"orderNo"}, expireSeconds = 3600,
            deleteOnSuccess = true, concurrentStrategy = Idempotent.ConcurrentStrategy.THROW)
    public Result<PaymentResponse> create(@RequestBody @Valid PaymentCreateRequest request) {
        return Result.success(paymentService.createPayment(request));
    }

    @PostMapping("/callback")
    @Idempotent(keyPrefix = "payment:callback", fields = {"paymentNo"}, expireSeconds = 86400)
    public Result<PaymentResponse> callback(@RequestBody @Valid PaymentCallbackRequest request) {
        return Result.success(paymentService.handleCallback(request));
    }

    @GetMapping("/query/{paymentId}")
    public Result<PaymentResponse> query(@PathVariable String paymentId) {
        return Result.success(paymentService.queryPayment(paymentId));
    }

    @GetMapping("/queryByOrder/{orderId}")
    public Result<PaymentResponse> queryByOrder(@PathVariable String orderId) {
        return Result.success(paymentService.queryByOrderId(orderId));
    }

    @PostMapping("/refund")
    @Idempotent(keyPrefix = "payment:refund", fields = {"refundNo"}, expireSeconds = 3600, deleteOnSuccess = true)
    public Result<RefundResponse> refund(@RequestBody @Valid RefundRequest request) {
        return Result.success(paymentService.applyRefund(request));
    }
}
