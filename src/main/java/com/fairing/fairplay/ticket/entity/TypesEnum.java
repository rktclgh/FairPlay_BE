package com.fairing.fairplay.ticket.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TypesEnum {
    EVENT,
    BOOTH;

    @JsonCreator
    public static TypesEnum from(String value) {
        return TypesEnum.valueOf(value.toUpperCase());
    }
}
