package com.hotel.checkin.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CheckInRequest {
    @NotBlank
    private String orderId;
    @NotBlank
    private String hotelId;
    @NotBlank
    private String roomId;
    private String memberId;
    private List<GuestInfo> guests;
    private Integer checkinChannel;
    private String strategy;

    @Data
    public static class GuestInfo {
        @NotBlank
        private String guestName;
        @NotNull
        private Integer guestType;
        @NotNull
        private Integer idCardType;
        @NotBlank
        private String idCardNo;
        private Integer gender;
        private String phone;
    }
}
