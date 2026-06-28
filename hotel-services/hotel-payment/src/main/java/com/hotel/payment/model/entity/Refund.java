package com.hotel.payment.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pay_refund")
public class Refund {
    @TableId
    private String refundId;
    private String refundNo;
    private String paymentId;
    private String orderId;
    private String hotelId;
    private Integer refundType;
    private BigDecimal refundAmount;
    private String refundReason;
    private Integer refundStatus;
    private LocalDateTime refundTime;
    private String operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
