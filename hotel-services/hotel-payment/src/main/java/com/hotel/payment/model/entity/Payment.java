package com.hotel.payment.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pay_payment")
public class Payment {
    @TableId
    private String paymentId;
    private String paymentNo;
    private String orderId;
    private String orderNo;
    private String hotelId;
    private String memberId;
    private Integer businessType;
    private String businessId;
    private Integer paymentType;
    private String payChannel;
    private BigDecimal amount;
    private String currency;
    private Integer status;
    private String tradeNo;
    private LocalDateTime payTime;
    private LocalDateTime expireTime;
    private String refundId;
    private BigDecimal refundAmount;
    private LocalDateTime refundTime;
    private String refundReason;
    private String clientIp;
    private String paymentParams;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
