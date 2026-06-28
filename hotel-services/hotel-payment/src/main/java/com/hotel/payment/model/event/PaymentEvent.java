package com.hotel.payment.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent implements Serializable {
    private String paymentId;
    private String paymentNo;
    private String orderId;
    private String orderNo;
    private String hotelId;
    private String eventType;
    private BigDecimal amount;
    private String tradeNo;
    private long timestamp;
}
