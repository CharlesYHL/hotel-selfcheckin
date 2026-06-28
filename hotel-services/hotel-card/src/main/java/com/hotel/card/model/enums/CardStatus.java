package com.hotel.card.model.enums;

import lombok.Getter;

@Getter
public enum CardStatus {
    ACTIVE(1, "有效"),
    EXPIRED(2, "已过期"),
    LOST(3, "已挂失"),
    CANCELLED(4, "已注销");

    private final int code;
    private final String desc;

    CardStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CardStatus of(int code) {
        for (CardStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown CardStatus code: " + code);
    }
}
