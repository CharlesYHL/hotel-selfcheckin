package com.hotel.room.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoomReleaseRequest {
    @NotBlank
    private String roomId;
    @NotBlank
    private String orderId;
}
