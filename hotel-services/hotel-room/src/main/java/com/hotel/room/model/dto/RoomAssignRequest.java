package com.hotel.room.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoomAssignRequest {
    @NotBlank
    private String orderId;
    @NotBlank
    private String hotelId;
    @NotBlank
    private String roomTypeId;
    private String strategy;
}
