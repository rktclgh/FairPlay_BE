package com.fairing.fairplay.common.exception;

import lombok.Getter;

@Getter
public class LinkExpiredException extends RuntimeException {
    public LinkExpiredException(String message) {
        super(message);
    }
}