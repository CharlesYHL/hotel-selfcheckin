package com.hotel.room.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {
    private String roomId;
    private String roomNo;
    private String hotelId;
    private String roomTypeId;
    private String floorNo;
    private Integer roomStatus;
    private String roomStatusDesc;
    private String direction;
    private Integer maxGuest;
    private Integer isSmokeFree;
    private LocalDateTime createdTime;
}
