package com.hotel.payment.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
    @NotBlank
    private String paymentId;
    @NotBlank
    private String refundNo;
    @NotBlank
    private String refundReason;
    @NotNull
    private BigDecimal refundAmount;
    private Integer refundType = 1;
}
