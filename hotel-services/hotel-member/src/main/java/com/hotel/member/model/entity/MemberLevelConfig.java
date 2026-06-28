package com.hotel.member.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("mem_level")
public class MemberLevelConfig {
    @TableId
    private String levelId;
    private String levelCode;
    private String levelName;
    private Integer levelType;
    private Long minPoints;
    private Long maxPoints;
    private BigDecimal pointsRate;
    private BigDecimal discountRate;
    private Integer advanceHours;
    private Integer lateCheckout;
    private Integer freeUpgrade;
    private Integer freeCancel;
    private Integer priorityCheckin;
    private Integer sortOrder;
    private Integer status;
    private java.time.LocalDateTime createdTime;
}
