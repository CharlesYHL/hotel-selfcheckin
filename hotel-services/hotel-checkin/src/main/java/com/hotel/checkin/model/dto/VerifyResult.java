package com.hotel.checkin.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyResult {
    private boolean passed;
    private String message;
    private Integer verifyStatus;
}
