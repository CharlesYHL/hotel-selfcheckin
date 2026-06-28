package com.hotel.checkin.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExtendStayRequest {
    @NotBlank
    private String checkinId;
    @NotNull
    private Integer extendDays;
}
