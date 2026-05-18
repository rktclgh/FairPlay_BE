package com.fairing.fairplay.attendee.service;

import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.attendee.repository.AttendeeRepositoryCustom;
import com.fairing.fairplay.attendee.repository.AttendeeTypeCodeRepository;
import com.fairing.fairplay.attendeeform.service.AttendeeFormService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.qr.service.QrTicketService;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.Users;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendeeServiceAuthorizationTest {

  @Mock
  private AttendeeRepository attendeeRepository;

  @Mock
  private AttendeeTypeCodeRepository attendeeTypeCodeRepository;

  @Mock
  private AttendeeRepositoryCustom attendeeRepositoryCustom;

  @Mock
  private AttendeeFormService attendeeFormService;

  @Mock
  private ReservationRepository reservationRepository;

  @Mock
  private QrTicketService qrTicketService;

  @Mock
  private EventRepository eventRepository;

  private AttendeeService attendeeService;

  @BeforeEach
  void setUp() {
    attendeeService = new AttendeeService(
        attendeeRepository,
        attendeeTypeCodeRepository,
        attendeeRepositoryCustom,
        attendeeFormService,
        reservationRepository,
        qrTicketService,
        eventRepository
    );
  }

  @Test
  void eventManagerCanReadAttendeesForOwnEvent() {
    when(eventRepository.findById(1L)).thenReturn(Optional.of(event(1L, 100L)));
    when(attendeeRepository.findByEventId(1L)).thenReturn(List.of());

    assertThat(attendeeService.getAttendeesByEvent(1L, user(100L, "EVENT_MANAGER"))).isEmpty();
  }

  @Test
  void eventManagerCannotReadAttendeesForOtherEventBeforeQuery() {
    when(eventRepository.findById(1L)).thenReturn(Optional.of(event(1L, 100L)));

    assertThatThrownBy(() -> attendeeService.getAttendeesByEvent(1L, user(999L, "EVENT_MANAGER")))
        .isInstanceOf(AccessDeniedException.class);

    verify(attendeeRepository, never()).findByEventId(any());
  }

  @Test
  void commonAndBoothManagerCannotReadEventAttendees() {
    assertThatThrownBy(() -> attendeeService.getAttendeesByEvent(1L, user(300L, "COMMON")))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> attendeeService.getAttendeesByEvent(1L, user(200L, "BOOTH_MANAGER")))
        .isInstanceOf(AccessDeniedException.class);

    verify(eventRepository, never()).findById(any());
    verify(attendeeRepository, never()).findByEventId(any());
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
