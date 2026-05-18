package com.fairing.fairplay.reservation.service;

import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.notification.service.NotificationService;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.payment.repository.PaymentStatusCodeRepository;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationLogRepository;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.ticket.repository.EventScheduleRepository;
import com.fairing.fairplay.ticket.repository.ScheduleTicketRepository;
import com.fairing.fairplay.ticket.repository.TicketRepository;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceAuthorizationTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private AttendeeRepository attendeeRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationLogRepository reservationLogRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EventScheduleRepository eventScheduleRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ScheduleTicketRepository scheduleTicketRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStatusCodeRepository paymentStatusCodeRepository;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                attendeeRepository,
                eventRepository,
                userRepository,
                reservationLogRepository,
                notificationService,
                eventScheduleRepository,
                ticketRepository,
                scheduleTicketRepository,
                paymentRepository,
                paymentStatusCodeRepository
        );
    }

    @Test
    void commonUserCanReadOnlyOwnReservationDetail() {
        Reservation reservation = reservation(10L, 1L, 300L, 100L);
        when(reservationRepository.findByReservationIdAndEvent_EventId(10L, 1L))
                .thenReturn(Optional.of(reservation));

        assertThat(reservationService.getReservationById(1L, 10L, user(300L, "COMMON")))
                .isSameAs(reservation);
    }

    @Test
    void commonUserCannotReadOtherReservationDetail() {
        Reservation reservation = reservation(10L, 1L, 301L, 100L);
        when(reservationRepository.findByReservationIdAndEvent_EventId(10L, 1L))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getReservationById(1L, 10L, user(300L, "COMMON")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void commonUserCannotReadEventReservationCollections() {
        assertThatThrownBy(() -> reservationService.getReservationsByEvent(1L, user(300L, "COMMON")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reservationService.getReservationAttendees(
                1L, null, null, null, null, PageRequest.of(0, 15), user(300L, "COMMON")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reservationService.generateAttendeesExcel(1L, null, user(300L, "COMMON")))
                .isInstanceOf(AccessDeniedException.class);

        verify(reservationRepository, never()).findByEvent_EventId(any());
        verify(attendeeRepository, never()).findAttendeesWithFilters(any(), any(), any(), any(), any(), any());
        verify(attendeeRepository, never()).findAttendeesByEventId(any(), any());
    }

    @Test
    void eventManagerCanReadOwnEventReservationsAndAttendees() {
        Event event = event(1L, 100L);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(reservationRepository.findByEvent_EventId(1L)).thenReturn(List.of());
        when(attendeeRepository.findAttendeesWithFilters(1L, null, null, null, null, PageRequest.of(0, 15)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        assertThat(reservationService.getReservationsByEvent(1L, user(100L, "EVENT_MANAGER"))).isEmpty();
        assertThat(reservationService.getReservationAttendees(
                1L, null, null, null, null, PageRequest.of(0, 15), user(100L, "EVENT_MANAGER"))).isEmpty();
    }

    @Test
    void eventManagerCannotReadOtherEventBeforePersonalDataQuery() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event(1L, 100L)));

        assertThatThrownBy(() -> reservationService.getReservationAttendees(
                1L, null, null, null, null, PageRequest.of(0, 15), user(999L, "EVENT_MANAGER")))
                .isInstanceOf(AccessDeniedException.class);

        verify(attendeeRepository, never()).findAttendeesWithFilters(any(), any(), any(), any(), any(), any());
    }

    @Test
    void eventManagerCannotDownloadOtherEventExcelBeforePersonalDataQuery() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event(1L, 100L)));

        assertThatThrownBy(() -> reservationService.generateAttendeesExcel(1L, null, user(999L, "EVENT_MANAGER")))
                .isInstanceOf(AccessDeniedException.class);

        verify(attendeeRepository, never()).findAttendeesByEventId(any(), any());
    }

    @Test
    void boothManagerCannotReadEventReservationPersonalData() {
        assertThatThrownBy(() -> reservationService.getReservationAttendees(
                1L, null, null, null, null, PageRequest.of(0, 15), user(200L, "BOOTH_MANAGER")))
                .isInstanceOf(AccessDeniedException.class);

        verify(attendeeRepository, never()).findAttendeesWithFilters(any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminCanReadAnyEventReservationCollections() throws Exception {
        when(reservationRepository.findByEvent_EventId(1L)).thenReturn(List.of());
        when(attendeeRepository.findAttendeesByEventId(1L, null)).thenReturn(List.of());

        assertThat(reservationService.getReservationsByEvent(1L, user(1L, "ADMIN"))).isEmpty();
        assertThat(reservationService.generateAttendeesExcel(1L, null, user(1L, "ADMIN"))).isNotEmpty();

        verify(eventRepository, never()).findById(any());
    }

    @Test
    void reservationDetailUsesEventScopedLookup() {
        when(reservationRepository.findByReservationIdAndEvent_EventId(10L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.getReservationById(2L, 10L, mock(CustomUserDetails.class)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Reservation reservation(Long reservationId, Long eventId, Long reservationUserId, Long eventManagerId) {
        Reservation reservation = new Reservation(event(eventId, eventManagerId), null, null, new Users(reservationUserId), 1, 1000);
        reservation.setReservationId(reservationId);
        return reservation;
    }

    private Event event(Long eventId, Long managerUserId) {
        EventAdmin eventAdmin = new EventAdmin();
        eventAdmin.setUser(new Users(managerUserId));
        eventAdmin.setUserId(managerUserId);

        Event event = new Event();
        event.setEventId(eventId);
        event.setManager(eventAdmin);
        event.setTitleKr("행사");
        event.setTitleEng("Event");
        return event;
    }

    private CustomUserDetails user(Long userId, String roleCode) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getRoleCode()).thenReturn(roleCode);
        return userDetails;
    }
}
