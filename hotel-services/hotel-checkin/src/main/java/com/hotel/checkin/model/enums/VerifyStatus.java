package com.hotel.checkin.model.enums;

import lombok.Getter;

@Getter
public enum VerifyStatus {
    PENDING(1, "待核验"),
    PASSED(2, "已通过"),
    FAILED(3, "未通过"),
    MANUAL_REVIEW(4, "待人工");

    private final int code;
    private final String desc;

    VerifyStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static VerifyStatus of(int code) {
        for (VerifyStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown VerifyStatus code: " + code);
    }
}
