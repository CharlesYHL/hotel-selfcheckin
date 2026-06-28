package com.hotel.room.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_floor")
public class Floor {
    @TableId
    private String floorId;
    private String hotelId;
    private String floorNo;
    private String floorName;
    private Integer floorType;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdTime;
}
