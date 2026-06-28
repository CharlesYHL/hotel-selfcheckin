package com.hotel.card.model.enums;

import lombok.Getter;

@Getter
public enum CardActionType {
    CREATE(1, "制作"),
    OPEN(2, "开门"),
    LOST(3, "挂失"),
    REPLACE(4, "补办"),
    EXTEND(5, "延期"),
    CANCEL(6, "注销");

    private final int code;
    private final String desc;

    CardActionType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
