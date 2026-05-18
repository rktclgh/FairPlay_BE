package com.fairing.fairplay.event.service;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventManagerRepository;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.Refund;
import com.fairing.fairplay.payment.repository.RefundRepository;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventManagerServiceRefundAuthorizationTest {

    @Mock
    private EventManagerRepository eventManagerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefundRepository refundRepository;

    private EventManagerService eventManagerService;

    @BeforeEach
    void setUp() {
        eventManagerService = new EventManagerService(
                eventManagerRepository,
                userRepository,
                refundRepository
        );
    }

    @Test
    void refundOwnershipUsesRefundPaymentEventManager() {
        when(refundRepository.findById(1L))
                .thenReturn(Optional.of(refund(payment(event(10L, 100L)))));

        assertThat(eventManagerService.isRefundInManagedEvent(1L, 100L)).isTrue();
        assertThat(eventManagerService.isRefundInManagedEvent(1L, 999L)).isFalse();
    }

    @Test
    void refundWithoutEventIsNotOwnedByEventManager() {
        when(refundRepository.findById(1L))
                .thenReturn(Optional.of(refund(payment(null))));

        assertThat(eventManagerService.isRefundInManagedEvent(1L, 100L)).isFalse();
    }

    private Refund refund(Payment payment) {
        return Refund.builder()
                .refundId(1L)
                .payment(payment)
                .build();
    }

    private Payment payment(Event event) {
        return Payment.builder()
                .paymentId(10L)
                .event(event)
                .build();
    }

    private Event event(Long eventId, Long managerUserId) {
        EventAdmin eventAdmin = new EventAdmin();
        eventAdmin.setUserId(managerUserId);
        eventAdmin.setUser(new Users(managerUserId));

        Event event = new Event();
        event.setEventId(eventId);
        event.setManager(eventAdmin);
        return event;
    }
}
