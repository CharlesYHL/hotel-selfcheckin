package com.hotel.checkin.model.enums;

import lombok.Getter;

@Getter
public enum IdCardType {
    ID_CARD(1, "身份证"),
    PASSPORT(2, "护照"),
    HK_MACAU(3, "港澳通行证");

    private final int code;
    private final String desc;

    IdCardType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
