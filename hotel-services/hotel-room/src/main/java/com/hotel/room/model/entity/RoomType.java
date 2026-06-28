package com.hotel.room.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_room_type")
public class RoomType {
    @TableId
    private String roomTypeId;
    private String hotelId;
    private String roomTypeCode;
    private String roomTypeName;
    private String roomTypeShort;
    private String description;
    private Integer baseCapacity;
    private Integer maxCapacity;
    private String bedType;
    private String bedSize;
    private BigDecimal roomArea;
    private String floorRange;
    private Integer maxRooms;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    @Version
    private Integer version;
}
