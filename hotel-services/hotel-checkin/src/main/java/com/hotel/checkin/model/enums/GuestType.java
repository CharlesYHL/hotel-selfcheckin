package com.hotel.checkin.model.enums;

import lombok.Getter;

@Getter
public enum GuestType {
    PRIMARY(1, "主入住人"),
    COMPANION(2, "同行人");

    private final int code;
    private final String desc;

    GuestType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
