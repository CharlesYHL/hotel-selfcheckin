package com.hotel.checkin.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("chk_checkin")
public class CheckIn {
    @TableId
    private String checkinId;
    private String checkinNo;
    private String orderId;
    private String orderNo;
    private String hotelId;
    private String memberId;
    private String roomId;
    private String roomNo;
    private String roomTypeId;
    private String roomTypeName;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalDateTime actualCheckoutTime;
    private Integer adults;
    private Integer children;
    private String cardNo;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;
    private Integer status;
    private Integer checkinChannel;
    private Integer verifyStatus;
    private String verifyRemark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    @com.baomidou.mybatisplus.annotation.Version
    private Integer version;
}
