package com.hotel.payment.model.enums;

public enum PaymentStatus {
    PENDING(1, "待支付"),
    PAYING(2, "支付中"),
    PAID(3, "已支付"),
    REFUNDED(4, "已退款"),
    FAILED(5, "支付失败"),
    CANCELLED(6, "已取消");

    private final int code;
    private final String desc;

    PaymentStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static PaymentStatus of(int code) {
        for (PaymentStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知支付状态: " + code);
    }
}
