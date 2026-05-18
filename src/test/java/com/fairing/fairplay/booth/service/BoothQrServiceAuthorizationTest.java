package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.booth.dto.BoothEntryRequestDto;
import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.entity.BoothExperience;
import com.fairing.fairplay.booth.entity.BoothExperienceReservation;
import com.fairing.fairplay.booth.entity.BoothExperienceStatusCode;
import com.fairing.fairplay.booth.repository.BoothExperienceRepository;
import com.fairing.fairplay.booth.repository.BoothExperienceReservationRepository;
import com.fairing.fairplay.booth.repository.BoothExperienceStatusCodeRepository;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.qr.dto.scan.CheckResponseDto;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.service.QrTicketEntryService;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.user.entity.BoothAdmin;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoothQrServiceAuthorizationTest {

  @Mock
  private BoothExperienceRepository boothExperienceRepository;

  @Mock
  private BoothExperienceReservationRepository reservationRepository;

  @Mock
  private BoothExperienceStatusCodeRepository statusCodeRepository;

  @Mock
  private BoothRepository boothRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private QrTicketEntryService qrTicketEntryService;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  private BoothQrService boothQrService;

  @BeforeEach
  void setUp() {
    BoothExperienceService boothExperienceService = new BoothExperienceService(
        boothExperienceRepository,
        reservationRepository,
        statusCodeRepository,
        boothRepository,
        userRepository,
        messagingTemplate
    );
    boothQrService = new BoothQrService(
        userRepository,
        statusCodeRepository,
        boothExperienceService,
        qrTicketEntryService,
        messagingTemplate
    );
  }

  @Test
  void checkInRejectsCommonUserBeforeStatusSave() {
    BoothExperienceReservation reservation = givenReadyReservationForManagedBooth();

    assertThatThrownBy(() -> boothQrService.checkIn(request(), 300L, "COMMON"))
        .isInstanceOf(CustomException.class)
        .extracting("status")
        .isEqualTo(HttpStatus.FORBIDDEN);

    verify(reservationRepository, never()).save(any());
    verify(qrTicketEntryService, never()).processQrEntry(any());
    assertThat(reservation.getExperienceStatusCode().getCode()).isEqualTo("READY");
  }

  @Test
  void checkInRejectsBoothManagerFromDifferentBoothBeforeStatusSave() {
    BoothExperienceReservation reservation = givenReadyReservationForManagedBooth();

    assertThatThrownBy(() -> boothQrService.checkIn(request(), 999L, "BOOTH_MANAGER"))
        .isInstanceOf(CustomException.class)
        .extracting("status")
        .isEqualTo(HttpStatus.FORBIDDEN);

    verify(reservationRepository, never()).save(any());
    verify(qrTicketEntryService, never()).processQrEntry(any());
    assertThat(reservation.getExperienceStatusCode().getCode()).isEqualTo("READY");
  }

  @Test
  void checkInAllowsOwningEventManagerToMoveReservationInProgress() {
    BoothExperienceReservation reservation = givenReadyReservationForManagedBooth();
    when(reservationRepository.save(reservation)).thenReturn(reservation);
    when(qrTicketEntryService.processQrEntry(any(QrTicket.class))).thenReturn(
        CheckResponseDto.builder()
            .message("입장 완료")
            .checkInTime(LocalDateTime.of(2026, 5, 18, 12, 0))
            .build()
    );

    assertThat(boothQrService.checkIn(request(), 100L, "EVENT_MANAGER").getMessage())
        .isEqualTo("입장 완료");

    verify(reservationRepository).save(reservation);
    verify(qrTicketEntryService).processQrEntry(any(QrTicket.class));
    assertThat(reservation.getExperienceStatusCode().getCode()).isEqualTo("IN_PROGRESS");
  }

  @Test
  void checkInAllowsAdminToMoveReservationInProgress() {
    BoothExperienceReservation reservation = givenReadyReservationForManagedBooth();
    when(reservationRepository.save(reservation)).thenReturn(reservation);
    when(qrTicketEntryService.processQrEntry(any(QrTicket.class))).thenReturn(
        CheckResponseDto.builder()
            .message("입장 완료")
            .checkInTime(LocalDateTime.of(2026, 5, 18, 12, 0))
            .build()
    );

    assertThat(boothQrService.checkIn(request(), 999L, "ADMIN").getMessage())
        .isEqualTo("입장 완료");

    verify(reservationRepository).save(reservation);
    verify(qrTicketEntryService).processQrEntry(any(QrTicket.class));
    assertThat(reservation.getExperienceStatusCode().getCode()).isEqualTo("IN_PROGRESS");
  }

  private BoothExperienceReservation givenReadyReservationForManagedBooth() {
    BoothExperience experience = experience(1L, booth(10L, 100L, 200L));
    BoothExperienceReservation reservation = reservation(5L, experience, "READY");
    QrTicket qrTicket = qrTicket(reservation);
    when(qrTicketEntryService.validateQrTicket(any(BoothEntryRequestDto.class))).thenReturn(qrTicket);
    when(boothExperienceRepository.findById(1L)).thenReturn(Optional.of(experience));
    when(reservationRepository.findLatestReadyReservation(1L, 10L, 300L)).thenReturn(Optional.of(reservation));
    when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
    when(statusCodeRepository.findByCode("IN_PROGRESS")).thenReturn(Optional.of(status("IN_PROGRESS")));
    return reservation;
  }

  private BoothEntryRequestDto request() {
    return BoothEntryRequestDto.builder()
        .eventId(1L)
        .boothId(10L)
        .boothExperienceId(1L)
        .qrCode("qr-code")
        .build();
  }

  private BoothExperience experience(Long experienceId, Booth booth) {
    return BoothExperience.builder()
        .experienceId(experienceId)
        .booth(booth)
        .title("체험")
        .description("설명")
        .experienceDate(LocalDate.now().plusDays(7))
        .startTime(LocalTime.of(10, 0))
        .endTime(LocalTime.of(11, 0))
        .durationMinutes(10)
        .maxCapacity(5)
        .allowWaiting(true)
        .maxWaitingCount(10)
        .allowDuplicateReservation(false)
        .isReservationEnabled(true)
        .reservations(List.of())
        .build();
  }

  private Booth booth(Long boothId, Long eventManagerId, Long boothManagerId) {
    EventAdmin eventAdmin = new EventAdmin();
    eventAdmin.setUser(new Users(eventManagerId));
    eventAdmin.setUserId(eventManagerId);

    Event event = new Event();
    event.setEventId(1L);
    event.setManager(eventAdmin);
    event.setTitleKr("행사");
    event.setTitleEng("Event");

    BoothAdmin boothAdmin = new BoothAdmin();
    boothAdmin.setUser(new Users(boothManagerId));
    boothAdmin.setUserId(boothManagerId);

    Booth booth = new Booth();
    booth.setId(boothId);
    booth.setEvent(event);
    booth.setBoothAdmin(boothAdmin);
    booth.setBoothTitle("부스");
    booth.setBoothDescription("설명");
    booth.setStartDate(LocalDate.now());
    booth.setEndDate(LocalDate.now().plusDays(1));
    return booth;
  }

  private BoothExperienceReservation reservation(Long reservationId, BoothExperience experience,
      String statusCode) {
    Users user = Users.builder()
        .userId(300L)
        .email("user@example.com")
        .name("참가자")
        .nickname("참가자")
        .phone("010-0000-0000")
        .password("pw")
        .build();
    BoothExperienceReservation reservation = BoothExperienceReservation.builder()
        .boothExperience(experience)
        .experienceStatusCode(status(statusCode))
        .user(user)
        .queuePosition(1)
        .build();
    reservation.setReservationId(reservationId);
    return reservation;
  }

  private QrTicket qrTicket(BoothExperienceReservation boothReservation) {
    Reservation reservation = mock(Reservation.class);
    when(reservation.getUser()).thenReturn(boothReservation.getUser());

    Attendee attendee = Attendee.builder()
        .reservation(reservation)
        .name("참가자")
        .phone("010-0000-0000")
        .email("user@example.com")
        .build();

    return QrTicket.builder()
        .id(99L)
        .attendee(attendee)
        .qrCode("qr-code")
        .ticketNo("TICKET-1")
        .expiredAt(LocalDateTime.now().plusDays(1))
        .build();
  }

  private BoothExperienceStatusCode status(String code) {
    BoothExperienceStatusCode statusCode = new BoothExperienceStatusCode();
    statusCode.setCode(code);
    statusCode.setName(code);
    return statusCode;
  }
}
