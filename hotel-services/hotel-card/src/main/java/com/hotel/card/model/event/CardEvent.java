package com.hotel.card.model.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CardEvent {
    private String cardId;
    private String cardNo;
    private String checkinId;
    private String hotelId;
    private String roomId;
    private String roomNo;
    private String eventType;  // CREATED, CANCELLED, LOST, REPLACED
    private LocalDateTime validTo;
    private Long timestamp;
}
