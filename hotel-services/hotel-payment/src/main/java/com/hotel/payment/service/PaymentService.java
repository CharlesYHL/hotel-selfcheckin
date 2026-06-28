package com.hotel.payment.service;

import com.hotel.common.cache.CacheService;
import com.hotel.common.core.BusinessException;
import com.hotel.payment.model.dto.*;
import com.hotel.payment.model.entity.Payment;
import com.hotel.payment.model.entity.Refund;
import com.hotel.payment.model.enums.PaymentStatus;
import com.hotel.payment.model.enums.RefundStatus;
import com.hotel.payment.model.event.PaymentEvent;
import com.hotel.payment.repository.PaymentMapper;
import com.hotel.payment.repository.RefundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CacheService cacheService;

    private static final Duration CALLBACK_CACHE_TTL = Duration.ofHours(24);

    /**
     * 创建支付订单。由 order-service 通过 Feign 调用。
     */
    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        Payment payment = new Payment();
        payment.setPaymentId("PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        payment.setPaymentNo("PN" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6));
        payment.setOrderId(request.getOrderId());
        payment.setOrderNo(request.getOrderNo());
        payment.setHotelId(request.getHotelId());
        payment.setMemberId(request.getMemberId());
        payment.setBusinessType(1);
        payment.setPaymentType(request.getPaymentType());
        payment.setPayChannel(request.getPayChannel());
        payment.setAmount(request.getAmount());
        payment.setCurrency("CNY");
        payment.setStatus(PaymentStatus.PENDING.getCode());
        payment.setExpireTime(LocalDateTime.now().plusMinutes(30));
        payment.setClientIp(null);
        payment.setCreatedTime(LocalDateTime.now());
        payment.setUpdatedTime(LocalDateTime.now());

        paymentMapper.insert(payment);
        log.info("支付订单创建: paymentId={}, paymentNo={}, orderId={}, amount={}",
                payment.getPaymentId(), payment.getPaymentNo(), payment.getOrderId(), payment.getAmount());

        return toResponse(payment);
    }

    /**
     * 处理支付回调。幂等：同一 paymentNo 多次回调只处理一次。
     * 通过 @Idempotent 注解保证方法级幂等，此方法仅处理业务逻辑。
     */
    @Transactional
    public PaymentResponse handleCallback(PaymentCallbackRequest request) {
        Payment payment = paymentMapper.selectByPaymentNo(request.getPaymentNo());
        if (payment == null) {
            throw new BusinessException("支付订单不存在: " + request.getPaymentNo());
        }

        if (payment.getStatus() == PaymentStatus.PAID.getCode()) {
            log.info("支付已处理，跳过: paymentNo={}", request.getPaymentNo());
            return toResponse(payment);
        }

        if (payment.getStatus() != PaymentStatus.PENDING.getCode()
                && payment.getStatus() != PaymentStatus.PAYING.getCode()) {
            throw new BusinessException("支付状态异常: " + PaymentStatus.of(payment.getStatus()).getDesc());
        }

        int updated = paymentMapper.updatePaid(payment.getPaymentId(),
                PaymentStatus.PAID.getCode(), request.getTradeNo(), payment.getStatus());
        if (updated == 0) {
            throw new BusinessException("支付状态更新失败，可能已被并发处理");
        }

        payment.setStatus(PaymentStatus.PAID.getCode());
        payment.setTradeNo(request.getTradeNo());
        payment.setPayTime(LocalDateTime.now());

        kafkaTemplate.send("payment-events", PaymentEvent.builder()
                .paymentId(payment.getPaymentId())
                .paymentNo(payment.getPaymentNo())
                .orderId(payment.getOrderId())
                .orderNo(payment.getOrderNo())
                .hotelId(payment.getHotelId())
                .eventType("PAID")
                .amount(payment.getAmount())
                .tradeNo(request.getTradeNo())
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("支付成功: paymentId={}, orderId={}, tradeNo={}", payment.getPaymentId(), payment.getOrderId(), request.getTradeNo());
        return toResponse(payment);
    }

    /**
     * 申请退款。Saga 补偿时调用。
     */
    @Transactional
    public RefundResponse applyRefund(RefundRequest request) {
        Payment payment = paymentMapper.selectById(request.getPaymentId());
        if (payment == null) {
            throw new BusinessException("支付订单不存在");
        }
        if (payment.getStatus() != PaymentStatus.PAID.getCode()) {
            throw new BusinessException("仅已支付订单可退款");
        }
        if (request.getRefundAmount().compareTo(payment.getAmount()) > 0) {
            throw new BusinessException("退款金额不能超过支付金额");
        }

        Refund refund = new Refund();
        refund.setRefundId("RFD" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        refund.setRefundNo(request.getRefundNo());
        refund.setPaymentId(payment.getPaymentId());
        refund.setOrderId(payment.getOrderId());
        refund.setHotelId(payment.getHotelId());
        refund.setRefundType(request.getRefundType());
        refund.setRefundAmount(request.getRefundAmount());
        refund.setRefundReason(request.getRefundReason());
        refund.setRefundStatus(RefundStatus.COMPLETED.getCode());
        refund.setRefundTime(LocalDateTime.now());
        refund.setCreatedTime(LocalDateTime.now());
        refund.setUpdatedTime(LocalDateTime.now());
        refundMapper.insert(refund);

        paymentMapper.updateRefunded(payment.getPaymentId(),
                PaymentStatus.REFUNDED.getCode(),
                refund.getRefundId(), refund.getRefundAmount(),
                refund.getRefundTime(), refund.getRefundReason(),
                PaymentStatus.PAID.getCode());

        kafkaTemplate.send("payment-events", PaymentEvent.builder()
                .paymentId(payment.getPaymentId())
                .paymentNo(payment.getPaymentNo())
                .orderId(payment.getOrderId())
                .orderNo(payment.getOrderNo())
                .hotelId(payment.getHotelId())
                .eventType("REFUNDED")
                .amount(request.getRefundAmount())
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("退款完成: refundId={}, paymentId={}, amount={}", refund.getRefundId(), payment.getPaymentId(), request.getRefundAmount());

        return RefundResponse.builder()
                .refundId(refund.getRefundId())
                .refundNo(refund.getRefundNo())
                .paymentId(refund.getPaymentId())
                .orderId(refund.getOrderId())
                .refundAmount(refund.getRefundAmount())
                .refundStatus(refund.getRefundStatus())
                .refundStatusDesc(RefundStatus.COMPLETED.getDesc())
                .createdTime(refund.getCreatedTime())
                .build();
    }

    /**
     * 查询支付状态。
     */
    public PaymentResponse queryPayment(String paymentId) {
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new BusinessException("支付订单不存在");
        }
        return toResponse(payment);
    }

    /**
     * 按订单ID查询支付记录。
     */
    public PaymentResponse queryByOrderId(String orderId) {
        var payments = paymentMapper.selectByOrderId(orderId);
        if (payments.isEmpty()) {
            throw new BusinessException("未找到支付记录");
        }
        return toResponse(payments.get(0));
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .paymentNo(p.getPaymentNo())
                .orderId(p.getOrderId())
                .amount(p.getAmount())
                .status(p.getStatus())
                .statusDesc(PaymentStatus.of(p.getStatus()).getDesc())
                .tradeNo(p.getTradeNo())
                .payTime(p.getPayTime())
                .createdTime(p.getCreatedTime())
                .build();
    }
}
