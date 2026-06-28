package com.hotel.checkin.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("chk_checkout")
public class CheckOut {
    @TableId
    private String checkoutId;
    private String checkinId;
    private String hotelId;
    private String roomId;
    private String roomNo;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Integer nights;
    private BigDecimal roomAmount;
    private BigDecimal extraAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal refundAmount;
    private BigDecimal dueAmount;
    private Integer checkoutType;
    private Integer checkoutStatus;
    private Integer cardReturn;
    private Integer itemCheck;
    private String remark;
    private String operatorId;
    private LocalDateTime createdTime;
}
