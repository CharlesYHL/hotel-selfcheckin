package com.hotel.member.model.enums;

import lombok.Getter;

@Getter
public enum MemberStatus {
    ACTIVE(1, "正常"),
    FROZEN(2, "冻结"),
    CANCELLED(3, "注销");

    private final int code;
    private final String desc;

    MemberStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MemberStatus of(int code) {
        for (MemberStatus s : values()) {
            if (s.code == code) return s;
        }
        return ACTIVE;
    }
}
