package com.hotel.order.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentCreateRequest {
    @NotBlank
    private String orderId;
    @NotBlank
    private String orderNo;
    @NotNull
    private BigDecimal amount;
    private String channel = "WX_PAY";
    private String subject;
    private String returnUrl;
}
