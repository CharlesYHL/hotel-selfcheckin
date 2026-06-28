package com.hotel.member.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PointsResponse {
    private String memberId;
    private Long totalPoints;
    private Long availablePoints;
    private Long earned;
    private Long balanceAfter;
    private String description;
    private LocalDateTime createdTime;
}
