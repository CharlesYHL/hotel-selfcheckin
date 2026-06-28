package com.hotel.room.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomAssignResponse {
    private boolean success;
    private String message;
    private String roomId;
    private String roomNo;
    private String roomTypeId;
    private String floorNo;
    private String hotelId;
}
