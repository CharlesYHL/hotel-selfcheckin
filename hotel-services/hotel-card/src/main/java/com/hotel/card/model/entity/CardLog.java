package com.hotel.card.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crd_card_log")
public class CardLog {
    @TableId
    private String logId;
    private String cardId;
    private String cardNo;
    private String hotelId;
    private String roomId;
    private String roomNo;
    private Integer actionType;
    private String openDevice;
    private Integer openResult;
    private String failReason;
    private String operatorId;
    private Integer operatorType;
    private String remark;
    private LocalDateTime createdTime;
}
