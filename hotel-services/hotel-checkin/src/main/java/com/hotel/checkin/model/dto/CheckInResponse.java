package com.hotel.checkin.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CheckInResponse {
    private String checkinId;
    private String checkinNo;
    private String orderId;
    private String roomId;
    private String roomNo;
    private String hotelId;
    private String memberId;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Integer status;
    private String statusDesc;
    private Integer verifyStatus;
    private String verifyStatusDesc;
    private String cardNo;
    private LocalDateTime createdTime;
}
