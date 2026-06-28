package com.hotel.card.model.enums;

import lombok.Getter;

@Getter
public enum CardType {
    NEW(1, "新制"),
    REPLACE(2, "补办"),
    EXTEND(3, "延期");

    private final int code;
    private final String desc;

    CardType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
