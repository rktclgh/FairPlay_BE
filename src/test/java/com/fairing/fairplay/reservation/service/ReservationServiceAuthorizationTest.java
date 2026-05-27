package com.fairing.fairplay.reservation.service;

import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventTicketRepository;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.notification.service.NotificationService;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.payment.repository.PaymentStatusCodeRepository;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.entity.ReservationStatusCode;
import com.fairing.fairplay.reservation.entity.ReservationStatusCodeEnum;
import com.fairing.fairplay.reservation.repository.ReservationLogRepository;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.entity.EventTicketId;
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
    private EventTicketRepository eventTicketRepository;

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
                eventTicketRepository,
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
    void eventManagerCanReadOwnReservationDetailForUnmanagedEvent() {
        Reservation reservation = reservation(10L, 1L, 100L, 999L);
        when(reservationRepository.findByReservationIdAndEvent_EventId(10L, 1L))
                .thenReturn(Optional.of(reservation));

        assertThat(reservationService.getReservationById(1L, 10L, user(100L, "EVENT_MANAGER")))
                .isSameAs(reservation);
    }

    @Test
    void boothManagerCanReadOwnReservationDetailForUnmanagedEvent() {
        Reservation reservation = reservation(10L, 1L, 200L, 999L);
        when(reservationRepository.findByReservationIdAndEvent_EventId(10L, 1L))
                .thenReturn(Optional.of(reservation));

        assertThat(reservationService.getReservationById(1L, 10L, user(200L, "BOOTH_MANAGER")))
                .isSameAs(reservation);
    }

    @Test
    void eventManagerCannotReadOthersReservationDetailForUnmanagedEvent() {
        Reservation reservation = reservation(10L, 1L, 301L, 999L);
        when(reservationRepository.findByReservationIdAndEvent_EventId(10L, 1L))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getReservationById(1L, 10L, user(100L, "EVENT_MANAGER")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void boothManagerCannotReadOthersReservationDetail() {
        Reservation reservation = reservation(10L, 1L, 301L, 999L);
        when(reservationRepository.findByReservationIdAndEvent_EventId(10L, 1L))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getReservationById(1L, 10L, user(200L, "BOOTH_MANAGER")))
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

    @Test
    void updateReservationRejectsReservationOutsidePathEvent() {
        Reservation reservation = reservation(10L, 2L, 300L, 100L);
        reservation.setReservationStatusCode(new ReservationStatusCode(ReservationStatusCodeEnum.CONFIRMED.getId()));
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        com.fairing.fairplay.reservation.dto.ReservationRequestDto requestDto =
                new com.fairing.fairplay.reservation.dto.ReservationRequestDto();
        requestDto.setReservationId(10L);
        requestDto.setEventId(1L);
        requestDto.setQuantity(1);
        requestDto.setPrice(1000);

        assertThatThrownBy(() -> reservationService.updateReservation(requestDto, 300L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("예약 ID");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservationRejectsScheduleOutsidePathEventBeforeTicketOrStockLookup() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event(1L, 100L)));
        when(userRepository.findById(300L)).thenReturn(Optional.of(new Users(300L)));
        when(eventScheduleRepository.findByEvent_EventIdAndScheduleId(1L, 20L)).thenReturn(Optional.empty());

        com.fairing.fairplay.reservation.dto.ReservationRequestDto requestDto =
                new com.fairing.fairplay.reservation.dto.ReservationRequestDto();
        requestDto.setEventId(1L);
        requestDto.setScheduleId(20L);
        requestDto.setTicketId(99L);
        requestDto.setQuantity(1);
        requestDto.setPrice(1000);

        assertThatThrownBy(() -> reservationService.createReservation(requestDto, 300L, 500L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("일정");

        verify(ticketRepository, never()).findById(any());
        verify(scheduleTicketRepository, never()).findByIdWithPessimisticLock(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservationRejectsTicketOutsidePathEventBeforeStockLookup() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event(1L, 100L)));
        when(userRepository.findById(300L)).thenReturn(Optional.of(new Users(300L)));
        when(eventScheduleRepository.findByEvent_EventIdAndScheduleId(1L, 20L))
                .thenReturn(Optional.of(schedule(1L, 20L)));
        when(eventTicketRepository.existsById(new EventTicketId(99L, 1L))).thenReturn(false);

        com.fairing.fairplay.reservation.dto.ReservationRequestDto requestDto =
                new com.fairing.fairplay.reservation.dto.ReservationRequestDto();
        requestDto.setEventId(1L);
        requestDto.setScheduleId(20L);
        requestDto.setTicketId(99L);
        requestDto.setQuantity(1);
        requestDto.setPrice(1000);

        assertThatThrownBy(() -> reservationService.createReservation(requestDto, 300L, 500L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("티켓");

        verify(ticketRepository, never()).findById(any());
        verify(scheduleTicketRepository, never()).findByIdWithPessimisticLock(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservationRejectsReservationOutsidePathEventWithoutMutation() {
        Reservation reservation = reservation(10L, 2L, 300L, 100L);
        reservation.setReservationStatusCode(new ReservationStatusCode(ReservationStatusCodeEnum.CONFIRMED.getId()));
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, 10L, 300L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("예약 ID");

        assertThat(reservation.getReservationStatusCode().getId())
                .isEqualTo(ReservationStatusCodeEnum.CONFIRMED.getId());
        verify(scheduleTicketRepository, never()).increaseStock(any(), any(), any(Integer.class));
        verify(reservationRepository, never()).save(any());
        verify(reservationLogRepository, never()).save(any());
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

    private EventSchedule schedule(Long eventId, Long scheduleId) {
        EventSchedule schedule = new EventSchedule();
        schedule.setScheduleId(scheduleId);
        schedule.setEvent(event(eventId, 100L));
        return schedule;
    }

    private CustomUserDetails user(Long userId, String roleCode) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getRoleCode()).thenReturn(roleCode);
        return userDetails;
    }
}
