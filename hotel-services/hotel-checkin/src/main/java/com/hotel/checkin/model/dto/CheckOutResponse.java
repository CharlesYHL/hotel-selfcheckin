package com.hotel.checkin.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CheckOutResponse {
    private String checkoutId;
    private String checkinId;
    private String roomId;
    private String roomNo;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Integer nights;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal refundAmount;
    private BigDecimal dueAmount;
    private Integer checkoutStatus;
    private String checkoutStatusDesc;
    private LocalDateTime createdTime;
}
