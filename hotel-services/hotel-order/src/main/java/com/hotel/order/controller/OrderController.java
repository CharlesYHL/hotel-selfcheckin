package com.hotel.order.controller;

import com.hotel.common.core.Result;
import com.hotel.common.idempotent.Idempotent;
import com.hotel.order.model.dto.OrderCreateRequest;
import com.hotel.order.model.dto.OrderResponse;
import com.hotel.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    @Idempotent(keyPrefix = "order:create", fields = {"orderNo"}, expireSeconds = 3600,
            deleteOnSuccess = true, concurrentStrategy = Idempotent.ConcurrentStrategy.THROW)
    public Result<OrderResponse> create(@RequestBody @Valid OrderCreateRequest request) {
        return Result.success(orderService.createOrder(request));
    }

    @GetMapping("/query/{orderId}")
    public Result<OrderResponse> query(@PathVariable String orderId) {
        log.info("查询订单请求: orderId={}", orderId);
        return Result.success(orderService.queryOrder(orderId));
    }

    @PostMapping("/cancel/{orderId}")
    @Idempotent(keyPrefix = "order:cancel", fields = {"orderId"}, expireSeconds = 3600)
    public Result<OrderResponse> cancel(@PathVariable String orderId) {
        return Result.success(orderService.cancelOrder(orderId));
    }

    @PostMapping("/{orderId}/status/{status}")
    public Result<Void> updateStatus(@PathVariable String orderId, @PathVariable Integer status) {
        orderService.updateOrderStatus(orderId, com.hotel.order.model.enums.OrderStatus.values()[status - 1]);
        return Result.success();
    }
}
