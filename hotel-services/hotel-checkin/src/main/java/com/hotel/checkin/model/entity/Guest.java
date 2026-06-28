package com.hotel.checkin.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("chk_guest")
public class Guest {
    @TableId
    private String guestId;
    private String checkinId;
    private String hotelId;
    private String guestName;
    private Integer guestType;
    private Integer idCardType;
    private String idCardNo;
    private String idCardHash;
    private String idCardEncrypted;
    private Integer keyVersion;
    private String salt;
    private String idCardMasked;
    private Integer gender;
    private LocalDate birthDate;
    private String phone;
    private String nationality;
    private String address;
    private Integer verifyStatus;
    private String verifyMessage;
    private LocalDateTime createdTime;
}
