package com.hotel.card.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crd_card")
public class RoomCard {
    @TableId
    private String cardId;
    private String cardNo;
    private String hotelId;
    private String checkinId;
    private String roomId;
    private String roomNo;
    private String guestId;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer cardType;
    private Integer cardStatus;
    private Integer openCount;
    private LocalDateTime lastOpenTime;
    private LocalDateTime lostTime;
    private LocalDateTime cancelTime;
    private String qrCode;
    private LocalDateTime qrExpireTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    @com.baomidou.mybatisplus.annotation.Version
    private Integer version;
}
