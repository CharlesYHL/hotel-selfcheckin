package com.hotel.order.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ord_order")
public class Order {
    @TableId
    private String orderId;
    private String orderNo;
    private String hotelId;
    private String memberId;
    private Integer sourceType;
    private String sourceChannel;
    private String roomTypeId;
    private String roomTypeName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private Integer roomCount;
    private Integer adults;
    private Integer children;
    private String contactName;
    private String contactPhone;
    private String specialRequest;
    private BigDecimal orderAmount;
    private BigDecimal discountAmount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;
    private String currency;
    private Integer orderStatus;
    private LocalDateTime payExpireTime;
    private LocalDateTime paidTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
