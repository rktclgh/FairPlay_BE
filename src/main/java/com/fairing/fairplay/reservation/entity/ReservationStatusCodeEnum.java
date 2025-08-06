package com.fairing.fairplay.reservation.entity;

import lombok.Getter;

@Getter
public enum ReservationStatusCodeEnum {
    PENDING(1, "대기"),
    CONFIRMED(2, "확정"),
    CANCELLED(3, "취소"),
    REFUNDED(4, "환불");

    private final int id;
    private final String name;

    ReservationStatusCodeEnum(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // id로 Enum 찾기
    public static ReservationStatusCodeEnum fromId(int id) {
        for (ReservationStatusCodeEnum status : values()) {
            if (status.id == id) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown id: " + id);
    }
}
