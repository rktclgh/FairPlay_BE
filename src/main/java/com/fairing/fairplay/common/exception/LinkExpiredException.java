package com.fairing.fairplay.common.exception;

import lombok.Getter;

@Getter
public class LinkExpiredException extends RuntimeException {
    private final String redirectUrl;

    public LinkExpiredException(String message, String redirectUrl) {
        super(message);
        this.redirectUrl = redirectUrl;
    }
}