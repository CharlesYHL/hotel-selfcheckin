package com.hotel.checkin.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("chk_extension")
public class Extension {
    @TableId
    private String extensionId;
    private String checkinId;
    private String hotelId;
    private String roomId;
    private LocalDateTime extendFrom;
    private LocalDateTime extendTo;
    private Integer extendDays;
    private BigDecimal nightlyRate;
    private BigDecimal extendAmount;
    private Integer payStatus;
    private String paymentId;
    private LocalDateTime createdTime;
}
