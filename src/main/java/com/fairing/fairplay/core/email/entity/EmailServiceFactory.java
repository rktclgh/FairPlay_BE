package com.fairing.fairplay.core.email.entity;

import com.fairing.fairplay.core.email.service.AbstractEmailService;
import com.fairing.fairplay.core.email.service.SendQrTicketEmailService;
import com.fairing.fairplay.core.email.service.TemporaryPasswordEmailService;
import com.fairing.fairplay.core.email.service.VerificationEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailServiceFactory {

  private final VerificationEmailService verificationEmailService;
  private final TemporaryPasswordEmailService temporaryPasswordEmailService;
  private final SendQrTicketEmailService sendQrTicketEmailService;

  public AbstractEmailService getService(EmailType type) {
    return switch (type) {
      case VERIFICATION -> verificationEmailService;
      case TEMPORARY_PASSWORD -> temporaryPasswordEmailService;
      case QR_TICKET -> sendQrTicketEmailService;
    };
  }

  public enum EmailType {
    VERIFICATION, TEMPORARY_PASSWORD, QR_TICKET
  }
}
