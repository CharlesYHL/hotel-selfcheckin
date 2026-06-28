package com.hotel.room.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("rom_price")
public class RoomPrice {
    @TableId
    private String priceId;
    private String roomTypeId;
    private String hotelId;
    private Integer dateType;
    private LocalDate priceDate;
    private BigDecimal rackRate;
    private BigDecimal sellRate;
    private BigDecimal costRate;
    private BigDecimal memberRate;
    private BigDecimal vipRate;
    private String currency;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
