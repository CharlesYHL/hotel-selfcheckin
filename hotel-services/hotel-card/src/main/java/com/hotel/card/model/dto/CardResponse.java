package com.hotel.card.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CardResponse {
    private String cardId;
    private String cardNo;
    private String hotelId;
    private String checkinId;
    private String roomId;
    private String roomNo;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer cardType;
    private String cardTypeDesc;
    private Integer cardStatus;
    private String cardStatusDesc;
    private Integer openCount;
    private String qrCode;
    private LocalDateTime createdTime;
}
