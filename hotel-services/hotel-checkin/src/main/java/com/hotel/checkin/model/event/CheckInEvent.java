package com.hotel.checkin.model.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CheckInEvent {
    private String checkinId;
    private String checkinNo;
    private String orderId;
    private String orderNo;
    private String hotelId;
    private String memberId;
    private String roomId;
    private String roomNo;
    private String roomTypeId;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String eventType;  // CHECKED_IN, EXTENDED, CHECKED_OUT
    private List<String> guestNames;
    private List<String> cardNumbers;
    private Long timestamp;
}
