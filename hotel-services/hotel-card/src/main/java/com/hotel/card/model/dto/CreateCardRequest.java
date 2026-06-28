package com.hotel.card.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCardRequest {
    @NotBlank
    private String checkinId;
    @NotBlank
    private String hotelId;
    @NotBlank
    private String roomId;
    @NotBlank
    private String roomNo;
    private String guestId;
    private java.time.LocalDateTime checkInTime;
    private java.time.LocalDateTime checkOutTime;
}
