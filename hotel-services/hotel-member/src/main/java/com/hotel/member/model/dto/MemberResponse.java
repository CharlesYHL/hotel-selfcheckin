package com.hotel.member.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MemberResponse {
    private String memberId;
    private String memberNo;
    private String nickname;
    private String avatar;
    private String memberName;
    private Integer gender;
    private String phone;
    private String email;
    private String levelId;
    private String levelName;
    private Long totalPoints;
    private Long availablePoints;
    private Integer totalStay;
    private Integer totalNights;
    private BigDecimal totalConsume;
    private BigDecimal balance;
    private Integer status;
    private String statusDesc;
    private LocalDateTime registerTime;
    private LocalDateTime createdTime;
}
