package com.hotel.room.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rom_room")
public class Room {
    @TableId
    private String roomId;
    private String roomNo;
    private String hotelId;
    private String roomTypeId;
    private String floorId;
    private String floorNo;
    private String building;
    private String unit;
    private Integer roomStatus;
    private String direction;
    private String windowType;
    private BigDecimal acreage;
    private String bedType;
    private Integer maxGuest;
    private Integer isSmokeFree;
    private Integer isActive;
    private Integer sortOrder;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    @Version
    private Integer version;
}
