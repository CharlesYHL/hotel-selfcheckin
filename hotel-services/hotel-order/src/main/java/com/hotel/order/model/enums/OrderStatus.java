package com.hotel.order.model.enums;

public enum OrderStatus {
    PENDING(1, "待支付"),
    PAID(2, "已支付"),
    ASSIGNED(3, "已排房"),
    CHECKED_IN(4, "已入住"),
    COMPLETED(5, "已完成"),
    CANCELLED(6, "已取消"),
    REFUNDED(7, "已退款");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
