package com.hotel.payment.model.dto;

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
    @NotBlank
    private String hotelId;
    private String memberId;
    @NotNull
    private BigDecimal amount;
    private Integer paymentType = 1;
    private String payChannel = "WX_PAY";
    private String businessType = "ORDER";
}
