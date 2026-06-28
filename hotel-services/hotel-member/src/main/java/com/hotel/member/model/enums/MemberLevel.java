package com.hotel.member.model.enums;

import lombok.Getter;

@Getter
public enum MemberLevel {
    BASIC(1, "基础会员"),
    SILVER(2, "银卡会员"),
    GOLD(3, "金卡会员"),
    PLATINUM(4, "白金会员"),
    DIAMOND(5, "钻石会员");

    private final int code;
    private final String desc;

    MemberLevel(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MemberLevel of(int code) {
        for (MemberLevel l : values()) {
            if (l.code == code) return l;
        }
        return BASIC;
    }
}
