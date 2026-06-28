package com.hotel.room.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeDTO {
    private String roomTypeId;
    private String hotelId;
    private String roomTypeCode;
    private String roomTypeName;
    private Integer baseCapacity;
    private Integer maxCapacity;
    private String bedType;
    private Integer availableCount;
    private Integer maxRooms;
}
