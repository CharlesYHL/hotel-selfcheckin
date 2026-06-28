package com.hotel.member.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("mem_points_log")
public class PointsLog {
    @TableId
    private String logId;
    private String memberId;
    private String hotelId;
    private Long points;
    private Long balanceBefore;
    private Long balanceAfter;
    private Integer businessType;
    private String businessId;
    private String orderId;
    private String orderNo;
    private String checkinId;
    private String description;
    private String operatorId;
    private LocalDate expireDate;
    private LocalDateTime createdTime;
}
