package com.hotel.member.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EarnPointsRequest {
    @NotBlank
    private String memberId;
    @NotNull
    private Long points;
    @NotNull
    private Integer businessType;
    private String businessId;
    private String orderId;
    private String orderNo;
    private String checkinId;
    private String description;
    private String operatorId;
}
