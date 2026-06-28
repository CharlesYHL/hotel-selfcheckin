package com.hotel.card.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CardActionRequest {
    @NotBlank
    private String cardId;
    @NotBlank
    private String hotelId;
    private String operatorId;
    private Integer operatorType;
    private String remark;
}
