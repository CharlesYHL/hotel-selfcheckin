package com.hotel.member.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("mem_member")
public class Member {
    @TableId
    private String memberId;
    private String memberNo;
    private String hotelId;
    private String openid;
    private String unionid;
    private String nickname;
    private String avatar;
    private String memberName;
    private Integer gender;
    private String phone;
    private String email;
    private String idCardNo;
    private String levelId;
    private String levelName;
    private Long totalPoints;
    private Long availablePoints;
    private Integer totalStay;
    private Integer totalNights;
    private BigDecimal totalConsume;
    private BigDecimal balance;
    private String firstHotelId;
    private LocalDate firstStayDate;
    private String lastHotelId;
    private LocalDate lastStayDate;
    private LocalDate birthday;
    private Integer memberSource;
    private Integer status;
    private LocalDateTime registerTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    @com.baomidou.mybatisplus.annotation.Version
    private Integer version;
}
