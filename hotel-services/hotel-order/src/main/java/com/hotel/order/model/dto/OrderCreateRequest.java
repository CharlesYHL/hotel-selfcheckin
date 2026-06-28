package com.hotel.order.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OrderCreateRequest {
    @NotBlank(message = "酒店ID不能为空")
    private String hotelId;

    private String memberId;

    @NotBlank(message = "房型ID不能为空")
    private String roomTypeId;

    @NotNull(message = "入住日期不能为空")
    private LocalDate checkInDate;

    @NotNull(message = "退房日期不能为空")
    private LocalDate checkOutDate;

    private Integer roomCount = 1;
    private Integer adults = 1;
    private Integer children = 0;
    private String contactName;
    private String contactPhone;
    private String specialRequest;
    private BigDecimal orderAmount;
    private Integer sourceType = 1;
    private String sourceChannel = "APP";
    private String orderNo;
}
