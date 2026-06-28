package com.hotel.payment.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentCallbackRequest {
    @NotBlank
    private String paymentNo;
    @NotBlank
    private String tradeNo;
    private Integer status;
    private String message;
}
