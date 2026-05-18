package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.booth.dto.BoothExperienceRequestDto;
import com.fairing.fairplay.booth.dto.BoothExperienceStatusUpdateDto;
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
import com.fairing.fairplay.user.entity.BoothAdmin;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import java.time.LocalDate;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoothExperienceServiceAuthorizationTest {

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
  private SimpMessagingTemplate messagingTemplate;

  private BoothExperienceService service;

  @BeforeEach
  void setUp() {
    service = new BoothExperienceService(
        boothExperienceRepository,
        reservationRepository,
        statusCodeRepository,
        boothRepository,
        userRepository,
        messagingTemplate
    );
  }

  @Test
  void createBoothExperienceRejectsEventManagerFromDifferentEvent() {
    Booth booth = booth(10L, 100L, 200L);
    when(boothRepository.findById(10L)).thenReturn(Optional.of(booth));

    assertThatThrownBy(() -> service.createBoothExperience(10L, request(), 999L, "EVENT_MANAGER"))
        .isInstanceOf(CustomException.class)
        .extracting("status")
        .isEqualTo(HttpStatus.FORBIDDEN);

    verify(boothExperienceRepository, never()).save(any());
  }

  @Test
  void createBoothExperienceAllowsOwningBoothManager() {
    Booth booth = booth(10L, 100L, 200L);
    when(boothRepository.findById(10L)).thenReturn(Optional.of(booth));
    when(boothExperienceRepository.save(any(BoothExperience.class))).thenAnswer(invocation -> {
      BoothExperience experience = invocation.getArgument(0);
      experience.setExperienceId(1L);
      return experience;
    });

    assertThat(service.createBoothExperience(10L, request(), 200L, "BOOTH_MANAGER").getExperienceId())
        .isEqualTo(1L);
  }

  @Test
  void updateBoothExperienceRejectsBoothManagerFromDifferentBooth() {
    BoothExperience experience = experience(1L, booth(10L, 100L, 200L));
    when(boothExperienceRepository.findById(1L)).thenReturn(Optional.of(experience));

    assertThatThrownBy(() -> service.updateBoothExperience(1L, request(), 999L, "BOOTH_MANAGER"))
        .isInstanceOf(CustomException.class)
        .extracting("status")
        .isEqualTo(HttpStatus.FORBIDDEN);

    verify(boothExperienceRepository, never()).save(any());
  }

  @Test
  void getExperienceReservationsRejectsEventManagerFromDifferentEvent() {
    BoothExperience experience = experience(1L, booth(10L, 100L, 200L));
    when(boothExperienceRepository.findById(1L)).thenReturn(Optional.of(experience));

    assertThatThrownBy(() -> service.getExperienceReservations(1L, 999L, "EVENT_MANAGER"))
        .isInstanceOf(CustomException.class)
        .extracting("status")
        .isEqualTo(HttpStatus.FORBIDDEN);

    verify(reservationRepository, never()).findByBoothExperienceOrderByReservedAt(any());
  }

  @Test
  void updateReservationStatusRejectsBoothManagerFromDifferentBooth() {
    BoothExperienceReservation reservation = reservation(5L, experience(1L, booth(10L, 100L, 200L)), "WAITING");
    when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

    BoothExperienceStatusUpdateDto updateDto = new BoothExperienceStatusUpdateDto();
    updateDto.setStatusCode("READY");

    assertThatThrownBy(() -> service.updateReservationStatus(5L, updateDto, 999L, "BOOTH_MANAGER"))
        .isInstanceOf(CustomException.class)
        .extracting("status")
        .isEqualTo(HttpStatus.FORBIDDEN);

    verify(statusCodeRepository, never()).findByCode(any());
    verify(reservationRepository, never()).save(any());
  }

  @Test
  void updateReservationStatusAllowsOwningEventManager() {
    BoothExperienceReservation reservation = reservation(5L, experience(1L, booth(10L, 100L, 200L)), "WAITING");
    BoothExperienceStatusCode ready = status("READY");
    when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
    when(statusCodeRepository.findByCode("READY")).thenReturn(Optional.of(ready));
    when(reservationRepository.save(reservation)).thenReturn(reservation);

    BoothExperienceStatusUpdateDto updateDto = new BoothExperienceStatusUpdateDto();
    updateDto.setStatusCode("READY");

    assertThat(service.updateReservationStatus(5L, updateDto, 100L, "EVENT_MANAGER").getReservationId())
        .isEqualTo(5L);
  }

  @Test
  void deleteBoothExperienceRejectsCommonUserBeforeDelete() {
    BoothExperience experience = experience(1L, booth(10L, 100L, 200L));
    when(boothExperienceRepository.findById(1L)).thenReturn(Optional.of(experience));

    assertThatThrownBy(() -> service.deleteBoothExperience(1L, 300L, "COMMON"))
        .isInstanceOf(CustomException.class)
        .extracting("status")
        .isEqualTo(HttpStatus.FORBIDDEN);

    verify(reservationRepository, never()).findActiveReservationsByExperience(any());
    verify(boothExperienceRepository, never()).delete(any());
  }

  @Test
  void deleteBoothExperienceAllowsOwningBoothManager() {
    BoothExperience experience = experience(1L, booth(10L, 100L, 200L));
    when(boothExperienceRepository.findById(1L)).thenReturn(Optional.of(experience));
    when(reservationRepository.findActiveReservationsByExperience(experience)).thenReturn(List.of());
    when(reservationRepository.findByBoothExperienceOrderByReservedAt(experience)).thenReturn(List.of());

    service.deleteBoothExperience(1L, 200L, "BOOTH_MANAGER");

    verify(boothExperienceRepository).delete(experience);
  }

  @Test
  void getBoothExperiencesReturnsOnlyRequestedBoothForOwningEventManager() {
    Booth booth = booth(10L, 100L, 200L);
    BoothExperience experience = experience(1L, booth);
    when(boothRepository.findById(10L)).thenReturn(Optional.of(booth));
    when(boothExperienceRepository.findByBooth_Id(10L)).thenReturn(List.of(experience));

    assertThat(service.getBoothExperiences(10L, 100L, "EVENT_MANAGER"))
        .extracting("experienceId")
        .containsExactly(1L);

    verify(boothExperienceRepository, never()).findByEventManagerId(any());
  }

  @Test
  void getManageableExperiencesReturnsEmptyForCommonUser() {
    assertThat(service.getManageableExperiences(300L, "COMMON")).isEmpty();

    verify(boothExperienceRepository, never()).findByEventManagerId(any());
    verify(boothExperienceRepository, never()).findByBoothAdminId(any());
  }

  private BoothExperienceRequestDto request() {
    BoothExperienceRequestDto requestDto = new BoothExperienceRequestDto();
    requestDto.setTitle("체험");
    requestDto.setDescription("설명");
    requestDto.setExperienceDate(LocalDate.now().plusDays(7));
    requestDto.setStartTime(LocalTime.of(10, 0));
    requestDto.setEndTime(LocalTime.of(11, 0));
    requestDto.setDurationMinutes(10);
    requestDto.setMaxCapacity(5);
    requestDto.setAllowWaiting(true);
    requestDto.setMaxWaitingCount(10);
    requestDto.setAllowDuplicateReservation(false);
    requestDto.setIsReservationEnabled(true);
    return requestDto;
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
    Users eventManager = new Users(eventManagerId);
    EventAdmin eventAdmin = new EventAdmin();
    eventAdmin.setUser(eventManager);
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

  private BoothExperienceReservation reservation(Long reservationId, BoothExperience experience, String statusCode) {
    BoothExperienceReservation reservation = BoothExperienceReservation.builder()
        .boothExperience(experience)
        .experienceStatusCode(status(statusCode))
        .user(new Users(300L))
        .queuePosition(1)
        .build();
    reservation.setReservationId(reservationId);
    return reservation;
  }

  private BoothExperienceStatusCode status(String code) {
    BoothExperienceStatusCode statusCode = new BoothExperienceStatusCode();
    statusCode.setCode(code);
    statusCode.setName(code);
    return statusCode;
  }
}
