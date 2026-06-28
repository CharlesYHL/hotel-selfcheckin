package com.hotel.order.service;

import com.hotel.common.core.BusinessException;
import com.hotel.common.core.Result;
import com.hotel.order.feign.PaymentFeignClient;
import com.hotel.order.feign.RoomFeignClient;
import com.hotel.order.model.dto.OrderCreateRequest;
import com.hotel.order.model.dto.OrderResponse;
import com.hotel.order.model.entity.Order;
import com.hotel.order.model.enums.OrderStatus;
import com.hotel.order.repository.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderMapper orderMapper;
    private final PaymentFeignClient paymentFeignClient;
    private final RoomFeignClient roomFeignClient;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        Order order = new Order();
        order.setOrderId("ORD" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        order.setOrderNo(request.getOrderNo() != null ? request.getOrderNo()
                : "ON" + System.currentTimeMillis());
        order.setHotelId(request.getHotelId());
        order.setMemberId(request.getMemberId());
        order.setRoomTypeId(request.getRoomTypeId());
        order.setCheckInDate(request.getCheckInDate());
        order.setCheckOutDate(request.getCheckOutDate());
        order.setNights((int) ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate()));
        order.setRoomCount(request.getRoomCount());
        order.setAdults(request.getAdults());
        order.setChildren(request.getChildren());
        order.setContactName(request.getContactName());
        order.setContactPhone(request.getContactPhone());
        order.setSpecialRequest(request.getSpecialRequest());
        order.setOrderAmount(request.getOrderAmount() != null ? request.getOrderAmount() : BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setDueAmount(order.getOrderAmount());
        order.setCurrency("CNY");
        order.setOrderStatus(OrderStatus.PENDING.getCode());
        order.setSourceType(request.getSourceType());
        order.setSourceChannel(request.getSourceChannel());
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(30));
        order.setCreatedTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());

        orderMapper.insert(order);
        log.info("订单创建成功: orderId={}, orderNo={}, amount={}", order.getOrderId(), order.getOrderNo(), order.getOrderAmount());

        return toResponse(order);
    }

    public OrderResponse queryOrder(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getOrderStatus() != OrderStatus.PENDING.getCode()) {
            throw new BusinessException("只有待支付订单可以取消");
        }
        order.setOrderStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单已取消: orderId={}", orderId);
        return toResponse(order);
    }

    @Transactional
    public void updateOrderStatus(String orderId, OrderStatus status) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setOrderStatus(status.getCode());
        order.setUpdatedTime(LocalDateTime.now());
        if (status == OrderStatus.PAID) {
            order.setPaidTime(LocalDateTime.now());
        }
        orderMapper.updateById(order);
    }

    private OrderResponse toResponse(Order order) {
        OrderStatus status = OrderStatus.values()[order.getOrderStatus() - 1];

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderNo(order.getOrderNo())
                .hotelId(order.getHotelId())
                .memberId(order.getMemberId())
                .roomTypeId(order.getRoomTypeId())
                .roomTypeName(order.getRoomTypeName())
                .checkInDate(order.getCheckInDate())
                .checkOutDate(order.getCheckOutDate())
                .nights(order.getNights())
                .roomCount(order.getRoomCount())
                .orderAmount(order.getOrderAmount())
                .paidAmount(order.getPaidAmount())
                .orderStatus(order.getOrderStatus())
                .orderStatusDesc(status.getDesc())
                .createdTime(order.getCreatedTime())
                .build();
    }
}
