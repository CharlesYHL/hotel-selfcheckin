package com.hotel.member.model.enums;

import lombok.Getter;

@Getter
public enum PointsBusinessType {
    CHECKIN(1, "入住"),
    CONSUME(2, "消费"),
    REFUND_DEDUCT(3, "退款扣减"),
    EXCHANGE(4, "兑换"),
    EXPIRE(5, "过期"),
    ADJUST(6, "调整");

    private final int code;
    private final String desc;

    PointsBusinessType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
