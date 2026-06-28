package com.hotel.member.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RegisterMemberRequest {
    @NotBlank
    private String phone;
    private String memberName;
    private Integer gender;
    private String email;
    private String openid;
    private String unionid;
    private String nickname;
    private String avatar;
    private LocalDate birthday;
    private Integer memberSource;
}
