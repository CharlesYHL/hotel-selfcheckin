package com.hotel.checkin.model.enums;

import lombok.Getter;

@Getter
public enum CheckInStatus {
    CHECKED_IN(1, "入住中"),
    EXTENDED(2, "已续住"),
    CHECKED_OUT(3, "已退房"),
    NO_SHOW(4, "取消入住");

    private final int code;
    private final String desc;

    CheckInStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CheckInStatus of(int code) {
        for (CheckInStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown CheckInStatus code: " + code);
    }
}
