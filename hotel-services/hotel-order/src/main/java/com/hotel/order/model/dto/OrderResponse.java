package com.hotel.order.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String orderId;
    private String orderNo;
    private String hotelId;
    private String memberId;
    private String roomTypeId;
    private String roomTypeName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Integer roomCount;
    private BigDecimal orderAmount;
    private BigDecimal paidAmount;
    private Integer orderStatus;
    private String orderStatusDesc;
    private LocalDateTime createdTime;
}
