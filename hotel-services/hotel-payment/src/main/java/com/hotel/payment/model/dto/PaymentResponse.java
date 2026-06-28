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
public class PaymentResponse {
    private String paymentId;
    private String paymentNo;
    private String orderId;
    private BigDecimal amount;
    private Integer status;
    private String statusDesc;
    private String tradeNo;
    private LocalDateTime payTime;
    private LocalDateTime createdTime;
}
