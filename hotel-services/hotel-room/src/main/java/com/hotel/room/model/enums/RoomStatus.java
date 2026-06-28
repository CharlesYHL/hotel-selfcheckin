package com.hotel.room.model.enums;

public enum RoomStatus {
    VACANT(1, "空房"),
    OCCUPIED(2, "占用"),
    MAINTENANCE(3, "维修"),
    CLEANING(4, "清洁"),
    RESERVED(5, "预订"),
    BLOCKED(6, "封房");

    private final int code;
    private final String desc;

    RoomStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static RoomStatus of(int code) {
        for (RoomStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知房态: " + code);
    }
}
