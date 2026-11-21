package com.hotel.payment.controller;

import com.hotel.common.core.Result;
import com.hotel.common.idempotent.Idempotent;
import com.hotel.payment.model.dto.*;
import com.hotel.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    @Idempotent(keyPrefix = "payment:create", fields = {"orderNo"}, expireSeconds = 3600,
            deleteOnSuccess = true, concurrentStrategy = Idempotent.ConcurrentStrategy.THROW)
    public Result<PaymentResponse> create(@RequestBody @Valid PaymentCreateRequest request) {
        log.info("[支付创建] 收到请求: orderId={}, orderNo={}, amount={}, payChannel={}",
                request.getOrderId(), request.getOrderNo(), request.getAmount(), request.getPayChannel());
        PaymentResponse response = paymentService.createPayment(request);
        log.info("[支付创建] 返回结果: paymentId={}, paymentNo={}", response.getPaymentId(), response.getPaymentNo());
        return Result.success(response);
    }

    @PostMapping("/callback")
    @Idempotent(keyPrefix = "payment:callback", fields = {"paymentNo"}, expireSeconds = 86400)
    public Result<PaymentResponse> callback(@RequestBody @Valid PaymentCallbackRequest request) {
        log.info("[支付回调] 收到请求: paymentNo={}, tradeNo={}, status={}",
                request.getPaymentNo(), request.getTradeNo(), request.getStatus());
        PaymentResponse response = paymentService.handleCallback(request);
        log.info("[支付回调] 处理完成: paymentId={}, orderId={}, tradeNo={}",
                response.getPaymentId(), response.getOrderId(), response.getTradeNo());
        return Result.success(response);
    }

    @GetMapping("/query/{paymentId}")
    public Result<PaymentResponse> query(@PathVariable String paymentId) {
        log.info("[支付查询] 收到请求: paymentId={}", paymentId);
        PaymentResponse response = paymentService.queryPayment(paymentId);
        log.info("[支付查询] 返回结果: paymentId={}, status={}", response.getPaymentId(), response.getStatusDesc());
        return Result.success(response);
    }

    @GetMapping("/queryByOrder/{orderId}")
    public Result<PaymentResponse> queryByOrder(@PathVariable String orderId) {
        log.info("[支付查询] 按订单查询: orderId={}", orderId);
        PaymentResponse response = paymentService.queryByOrderId(orderId);
        log.info("[支付查询] 按订单查询结果: paymentId={}, orderId={}, status={}",
                response.getPaymentId(), response.getOrderId(), response.getStatusDesc());
        return Result.success(response);
    }

    @PostMapping("/refund")
    @Idempotent(keyPrefix = "payment:refund", fields = {"refundNo"}, expireSeconds = 3600, deleteOnSuccess = true)
    public Result<RefundResponse> refund(@RequestBody @Valid RefundRequest request) {
        log.info("[退款申请] 收到请求: paymentId={}, refundNo={}, amount={}, reason={}",
                request.getPaymentId(), request.getRefundNo(), request.getRefundAmount(), request.getRefundReason());
        RefundResponse response = paymentService.applyRefund(request);
        log.info("[退款申请] 处理完成: refundId={}, paymentId={}, refundStatus={}",
                response.getRefundId(), response.getPaymentId(), response.getRefundStatusDesc());
        return Result.success(response);
    }
}
