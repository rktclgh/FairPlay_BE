package com.fairing.fairplay.core.email.entity;

import org.springframework.stereotype.Component;

import com.fairing.fairplay.core.email.service.AbstractEmailService;
import com.fairing.fairplay.core.email.service.SendQrTicketEmailService;
import com.fairing.fairplay.core.email.service.SuccessQrTicketEmailService;
import com.fairing.fairplay.core.email.service.TemporaryPasswordEmailService;
import com.fairing.fairplay.core.email.service.VerificationEmailService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailServiceFactory {

  private final VerificationEmailService verificationEmailService;
  private final TemporaryPasswordEmailService temporaryPasswordEmailService;
  private final SendQrTicketEmailService sendQrTicketEmailService;
  private final SuccessQrTicketEmailService successQrTicketEmailService;

  public AbstractEmailService getService(EmailType type) {
    return switch (type) {
      case VERIFICATION -> verificationEmailService;
      case TEMPORARY_PASSWORD -> temporaryPasswordEmailService;
      case SEND_QR_TICKET -> sendQrTicketEmailService;
      case SUCCESS_QR_TICKET -> successQrTicketEmailService;
    };
  }

  public enum EmailType {
    VERIFICATION, TEMPORARY_PASSWORD, SEND_QR_TICKET, SUCCESS_QR_TICKET
  }
}
