package com.fairing.fairplay.core.email.entity;

import com.fairing.fairplay.core.email.service.AbstractEmailService;
import com.fairing.fairplay.core.email.service.TemporaryPasswordEmailService;
import com.fairing.fairplay.core.email.service.VerificationEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailServiceFactory {
    private final VerificationEmailService verificationEmailService;
    private final TemporaryPasswordEmailService temporaryPasswordEmailService;

    public AbstractEmailService getService(EmailType type) {
        return switch (type) {
            case VERIFICATION -> verificationEmailService;
            case TEMPORARY_PASSWORD -> temporaryPasswordEmailService;
        };
    }

    public enum EmailType {
        VERIFICATION, TEMPORARY_PASSWORD
    }
}
