package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.admin.repository.AccountLevelRepository;
import com.fairing.fairplay.booth.dto.BoothCancelRequestDto;
import com.fairing.fairplay.booth.entity.BoothApplication;
import com.fairing.fairplay.booth.entity.BoothApplicationStatusCode;
import com.fairing.fairplay.booth.entity.BoothPaymentStatusCode;
import com.fairing.fairplay.booth.repository.BoothApplicationRepository;
import com.fairing.fairplay.booth.repository.BoothApplicationStatusCodeRepository;
import com.fairing.fairplay.booth.repository.BoothPaymentStatusCodeRepository;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.core.email.service.BoothEmailService;
import com.fairing.fairplay.core.service.UserSessionRevocationService;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.file.repository.FileRepository;
import com.fairing.fairplay.file.service.FileService;
import com.fairing.fairplay.notification.service.NotificationService;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.payment.service.RefundService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.BoothAdminRepository;
import com.fairing.fairplay.user.repository.EventAdminRepository;
import com.fairing.fairplay.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoothCancelServiceTest {

    @Mock
    private BoothApplicationRepository boothApplicationRepository;
    @Mock
    private BoothApplicationStatusCodeRepository statusCodeRepository;
    @Mock
    private BoothPaymentStatusCodeRepository paymentCodeRepository;
    @Mock
    private BoothRepository boothRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BoothAdminRepository boothAdminRepository;
    @Mock
    private AccountLevelRepository accountLevelRepository;
    @Mock
    private EventAdminRepository eventAdminRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private FileService fileService;
    @Mock
    private BoothEmailService boothEmailService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RefundService refundService;
    @Mock
    private UserSessionRevocationService userSessionRevocationService;

    @InjectMocks
    private BoothCancelService boothCancelService;

    @Test
    void requestBoothCancelRevokesDeletedBoothAdminSessions() {
        Event event = new Event();
        event.setEventId(1L);
        event.setTitleKr("행사");

        BoothApplicationStatusCode applicationStatus = org.mockito.Mockito.mock(BoothApplicationStatusCode.class);
        when(applicationStatus.getCode()).thenReturn("APPROVED");

        BoothPaymentStatusCode currentPaymentStatus = org.mockito.Mockito.mock(BoothPaymentStatusCode.class);
        when(currentPaymentStatus.getCode()).thenReturn("PAID");

        BoothPaymentStatusCode cancelledPaymentStatus = org.mockito.Mockito.mock(BoothPaymentStatusCode.class);

        BoothApplication application = new BoothApplication();
        application.setId(7L);
        application.setEvent(event);
        application.setContactEmail("booth@example.com");
        application.setBoothEmail("admin@example.com");
        application.setBoothTitle("A-1");
        application.setBoothApplicationStatusCode(applicationStatus);
        application.setBoothPaymentStatusCode(currentPaymentStatus);

        Users boothAdmin = new Users();
        boothAdmin.setUserId(42L);

        BoothCancelRequestDto request = new BoothCancelRequestDto();
        request.setContactEmail("booth@example.com");
        request.setCancelReason("cancel");

        when(boothApplicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(paymentCodeRepository.findByCode("CANCELLED")).thenReturn(Optional.of(cancelledPaymentStatus));
        when(paymentRepository.findByPaymentTargetType_PaymentTargetCodeAndTargetId("BOOTH_APPLICATION", 7L))
                .thenReturn(List.of());
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(boothAdmin));
        when(boothRepository.findByEventAndIsDeletedFalse(event)).thenReturn(List.of());
        when(boothAdminRepository.findByUser(boothAdmin)).thenReturn(Optional.empty());
        when(accountLevelRepository.findByUserId(42L)).thenReturn(null);
        when(fileRepository.findByTargetTypeAndTargetId("BOOTH_APPLICATION", 7L)).thenReturn(List.of());
        when(eventAdminRepository.findByEvent(event)).thenReturn(null);

        boothCancelService.requestBoothCancel(7L, request);

        verify(userRepository).save(boothAdmin);
        verify(userSessionRevocationService).revokeAfterCommit(42L);
    }
}
