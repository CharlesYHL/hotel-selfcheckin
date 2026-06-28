package com.hotel.payment.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private String refundId;
    private String refundNo;
    private String paymentId;
    private String orderId;
    private BigDecimal refundAmount;
    private Integer refundStatus;
    private String refundStatusDesc;
    private LocalDateTime createdTime;
}
