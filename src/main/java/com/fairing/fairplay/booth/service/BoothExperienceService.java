package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.entity.BoothExperience;
import com.fairing.fairplay.booth.entity.BoothExperienceReservation;
import com.fairing.fairplay.booth.entity.BoothExperienceStatusCode;
import com.fairing.fairplay.booth.repository.BoothExperienceRepository;
import com.fairing.fairplay.booth.repository.BoothExperienceReservationRepository;
import com.fairing.fairplay.booth.repository.BoothExperienceStatusCodeRepository;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoothExperienceService {

  private final BoothExperienceRepository boothExperienceRepository;
  private final BoothExperienceReservationRepository reservationRepository;
  private final BoothExperienceStatusCodeRepository statusCodeRepository;
  private final BoothRepository boothRepository;
  private final UserRepository userRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final BoothExperienceStatusCodeRepository boothExperienceStatusCodeRepository;

  // 1. 부스 체험 등록 (부스 담당자)
  @Transactional
  public BoothExperienceResponseDto createBoothExperience(Long boothId,
      BoothExperienceRequestDto requestDto) {
    log.info("부스 체험 등록 시작 - 부스 ID: {}", boothId);

    Booth booth = boothRepository.findById(boothId)
        .orElseThrow(() -> new IllegalArgumentException("부스를 찾을 수 없습니다: " + boothId));

    BoothExperience experience = BoothExperience.builder().booth(booth).title(requestDto.getTitle())
        .description(requestDto.getDescription()).experienceDate(requestDto.getExperienceDate())
        .startTime(requestDto.getStartTime()).endTime(requestDto.getEndTime())
        .durationMinutes(requestDto.getDurationMinutes()).maxCapacity(requestDto.getMaxCapacity())
        .allowWaiting(requestDto.getAllowWaiting() != null ? requestDto.getAllowWaiting() : true)
        .maxWaitingCount(requestDto.getMaxWaitingCount()).allowDuplicateReservation(
            requestDto.getAllowDuplicateReservation() != null
                ? requestDto.getAllowDuplicateReservation() : false).isReservationEnabled(
            requestDto.getIsReservationEnabled() != null ? requestDto.getIsReservationEnabled()
                : true).build();

    BoothExperience savedExperience = boothExperienceRepository.save(experience);
    log.info("부스 체험 등록 완료 - 체험 ID: {}", savedExperience.getExperienceId());

    return BoothExperienceResponseDto.fromEntity(savedExperience);
  }

  // 2. 부스 체험 목록 조회 (권한 기반)
  @Transactional(readOnly = true)
  public List<BoothExperienceResponseDto> getBoothExperiences(Long boothId, Long userId,
      String roleCode) {
    log.info("권한 기반 체험 목록 조회 - 부스 ID: {}, 사용자 ID: {}, 권한: {}", boothId, userId, roleCode);

    List<BoothExperience> experiences;

    if ("EVENT_MANAGER".equals(roleCode)) {
      // 행사 담당자: 해당 사용자가 관리하는 행사의 모든 부스 체험 조회
      experiences = boothExperienceRepository.findByEventManagerId(userId);
      log.info("EVENT_MANAGER - 관리 행사의 모든 체험 조회: {}개", experiences.size());
    } else if ("BOOTH_MANAGER".equals(roleCode)) {
      // 부스 담당자: 해당 사용자가 관리하는 부스의 체험만 조회
      experiences = boothExperienceRepository.findByBoothAdminId(userId);
      log.info("BOOTH_MANAGER - 관리 부스의 체험만 조회: {}개", experiences.size());
    } else {
      // 기타 권한: 빈 목록 반환
      log.warn("권한 없음 - 빈 목록 반환, 권한: {}", roleCode);
      experiences = List.of();
    }

    return experiences.stream().map(BoothExperienceResponseDto::fromEntity)
        .collect(Collectors.toList());
  }

  // 3. 체험 가능한 부스 체험 목록 조회 (참여자용)
  @Transactional(readOnly = true)
  public List<BoothExperienceResponseDto> getAvailableExperiences() {
    LocalDate today = LocalDate.now();
    List<BoothExperience> experiences = boothExperienceRepository.findAvailableExperiences(today);
    return experiences.stream().map(BoothExperienceResponseDto::fromEntity)
        .collect(Collectors.toList());
  }

  // 3-1. 체험 가능한 부스 체험 목록 조회 (필터링 지원)
  @Transactional(readOnly = true)
  public List<BoothExperienceResponseDto> getAvailableExperiences(Long eventId, String startDate,
      String endDate, String boothName, Boolean isAvailable, Long categoryId, String sortBy,
      String sortDirection) {

    log.info(
        "필터링된 체험 목록 조회 시작 - eventId: {}, startDate: {}, endDate: {}, boothName: {}, isAvailable: {}, sortBy: {}",
        eventId, startDate, endDate, boothName, isAvailable, sortBy);

    LocalDate today = LocalDate.now();

    // 날짜 파싱 - final 변수로 선언
    final LocalDate filterStartDate;
    final LocalDate filterEndDate;

    LocalDate startDateTemp;
    if (startDate != null && !startDate.trim().isEmpty()) {
      try {
        startDateTemp = LocalDate.parse(startDate);
      } catch (Exception e) {
        log.warn("잘못된 시작 날짜 형식: {}", startDate);
        startDateTemp = null;
      }
    } else {
      startDateTemp = null;
    }

    LocalDate endDateTemp;
    if (endDate != null && !endDate.trim().isEmpty()) {
      try {
        endDateTemp = LocalDate.parse(endDate);
      } catch (Exception e) {
        log.warn("잘못된 종료 날짜 형식: {}", endDate);
        endDateTemp = null;
      }
    } else {
      endDateTemp = null;
    }

    // 기본 조회 (체크박스 상태에 따라 쿼리 결정)
    filterStartDate = startDateTemp;
    filterEndDate = endDateTemp;

    List<BoothExperience> experiences;
    if (isAvailable != null && isAvailable) {
      // "예약 가능한 것만 보기" 체크된 경우: 예약 활성화된 것만 조회
      log.info("예약 활성화된 체험만 조회 - isAvailable: {}", isAvailable);
      experiences = boothExperienceRepository.findAvailableExperiences(today);
    } else {
      // 체크 해제되거나 null인 경우: 모든 체험 조회
      log.info("모든 체험 조회 - isAvailable: {}", isAvailable);
      experiences = boothExperienceRepository.findAllExperiences(today);
    }

    log.info("기본 쿼리 결과 - 조회된 체험 수: {} (활성화: {}, 비활성화: {})", experiences.size(),
        experiences.stream().mapToLong(exp -> exp.getIsReservationEnabled() ? 1 : 0).sum(),
        experiences.stream().mapToLong(exp -> exp.getIsReservationEnabled() ? 0 : 1).sum());

    // 필터링 적용
    List<BoothExperience> filteredExperiences = experiences.stream().filter(exp -> {
      // 이벤트 ID 필터
      if (eventId != null && !exp.getBooth().getEvent().getEventId().equals(eventId)) {
        return false;
      }

      // 날짜 기간 필터
      if (filterStartDate != null && exp.getExperienceDate().isBefore(filterStartDate)) {
        return false;
      }
      if (filterEndDate != null && exp.getExperienceDate().isAfter(filterEndDate)) {
        return false;
      }

      // 부스명/체험명 검색
      if (boothName != null && !boothName.trim().isEmpty()) {
        String searchTerm = boothName.toLowerCase();
        boolean matches =
            exp.getTitle().toLowerCase().contains(searchTerm) || (exp.getDescription() != null
                && exp.getDescription().toLowerCase().contains(searchTerm)) || exp.getBooth()
                .getBoothTitle().toLowerCase().contains(searchTerm);
        if (!matches) {
          return false;
        }
      }

      // 예약 활성화 여부 필터는 이미 쿼리 단계에서 처리됨

      return true;
    }).collect(Collectors.toList());

    // 정렬 적용
    sortExperiences(filteredExperiences, sortBy, sortDirection);

    List<BoothExperienceResponseDto> result = filteredExperiences.stream()
        .map(BoothExperienceResponseDto::fromEntity).collect(Collectors.toList());

    log.info("필터링된 체험 목록 조회 완료 - 총 {}개 (활성화: {}, 비활성화: {})", result.size(),
        result.stream().mapToLong(dto -> dto.getIsReservationEnabled() ? 1 : 0).sum(),
        result.stream().mapToLong(dto -> dto.getIsReservationEnabled() ? 0 : 1).sum());

    return result;
  }

  // 정렬 로직
  private void sortExperiences(List<BoothExperience> experiences, String sortBy,
      String sortDirection) {
    boolean ascending = "asc".equalsIgnoreCase(sortDirection);

    switch (sortBy.toLowerCase()) {
      case "starttime":
        experiences.sort((a, b) -> {
          int result = a.getStartTime().compareTo(b.getStartTime());
          return ascending ? result : -result;
        });
        break;
      case "congestionrate":
        experiences.sort((a, b) -> {
          int result = Double.compare(a.getCongestionRate(), b.getCongestionRate());
          return ascending ? result : -result;
        });
        break;
      case "createdat":
        experiences.sort((a, b) -> {
          int result = a.getCreatedAt().compareTo(b.getCreatedAt());
          return ascending ? result : -result;
        });
        break;
      case "experiencedate":
        experiences.sort((a, b) -> {
          int result = a.getExperienceDate().compareTo(b.getExperienceDate());
          return ascending ? result : -result;
        });
        break;
      default:
        // 기본 정렬: 체험 날짜 -> 시작 시간 순
        experiences.sort((a, b) -> {
          int dateResult = a.getExperienceDate().compareTo(b.getExperienceDate());
          if (dateResult != 0) {
            return ascending ? dateResult : -dateResult;
          }
          int timeResult = a.getStartTime().compareTo(b.getStartTime());
          return ascending ? timeResult : -timeResult;
        });
        break;
    }
  }

  // 3-1. 부스 체험 상세 조회
  @Transactional(readOnly = true)
  public BoothExperienceResponseDto getBoothExperience(Long experienceId) {
    log.info("부스 체험 상세 조회 - 체험 ID: {}", experienceId);

    BoothExperience experience = boothExperienceRepository.findById(experienceId)
        .orElseThrow(() -> new IllegalArgumentException("체험을 찾을 수 없습니다: " + experienceId));

    return BoothExperienceResponseDto.fromEntity(experience);
  }

  // 3-2. 부스 체험 수정 (부스 담당자)
  @Transactional
  public BoothExperienceResponseDto updateBoothExperience(Long experienceId,
      BoothExperienceRequestDto requestDto) {
    log.info("부스 체험 수정 시작 - 체험 ID: {}", experienceId);

    BoothExperience experience = boothExperienceRepository.findById(experienceId)
        .orElseThrow(() -> new IllegalArgumentException("체험을 찾을 수 없습니다: " + experienceId));

    // 기존 예약이 있는 경우 수정 제한 검증
    validateExperienceModification(experience);

    // 체험 정보 업데이트
    experience.updateExperience(requestDto.getTitle(), requestDto.getDescription(),
        requestDto.getExperienceDate(), requestDto.getStartTime(), requestDto.getEndTime(),
        requestDto.getDurationMinutes(), requestDto.getMaxCapacity(),
        requestDto.getAllowWaiting() != null ? requestDto.getAllowWaiting()
            : experience.getAllowWaiting(), requestDto.getMaxWaitingCount(),
        requestDto.getAllowDuplicateReservation() != null
            ? requestDto.getAllowDuplicateReservation() : experience.getAllowDuplicateReservation(),
        requestDto.getIsReservationEnabled() != null ? requestDto.getIsReservationEnabled()
            : experience.getIsReservationEnabled());

    BoothExperience updatedExperience = boothExperienceRepository.save(experience);
    log.info("부스 체험 수정 완료 - 체험 ID: {}", experienceId);

    return BoothExperienceResponseDto.fromEntity(updatedExperience);
  }

  // 3-3. 부스 체험 삭제 (부스 담당자)
  @Transactional
  public void deleteBoothExperience(Long experienceId) {
    log.info("부스 체험 삭제 시작 - 체험 ID: {}", experienceId);

    BoothExperience experience = boothExperienceRepository.findById(experienceId)
        .orElseThrow(() -> new IllegalArgumentException("체험을 찾을 수 없습니다: " + experienceId));

    // 활성 예약이 있는지 확인
    List<BoothExperienceReservation> activeReservations = reservationRepository.findActiveReservationsByExperience(
        experience);

    if (!activeReservations.isEmpty()) {
      throw new IllegalStateException("활성 예약이 있는 체험은 삭제할 수 없습니다. 예약을 먼저 처리해주세요.");
    }

    // 관련된 모든 예약 삭제 (히스토리 보존을 위해 soft delete 고려 가능)
    List<BoothExperienceReservation> allReservations = reservationRepository.findByBoothExperienceOrderByReservedAt(
        experience);

    if (!allReservations.isEmpty()) {
      reservationRepository.deleteAll(allReservations);
      log.info("체험 관련 예약 삭제 완료 - 예약 수: {}", allReservations.size());
    }

    // 체험 삭제
    boothExperienceRepository.delete(experience);
    log.info("부스 체험 삭제 완료 - 체험 ID: {}", experienceId);
  }

  // 체험 수정 가능 여부 검증
  private void validateExperienceModification(BoothExperience experience) {
    // 현재 진행중인 체험인지 확인
    LocalDate today = LocalDate.now();
    if (experience.getExperienceDate().equals(today)) {
      // 당일 체험은 기본 정보만 수정 가능하도록 제한할 수 있음
      log.warn("당일 체험 수정 요청 - 체험 ID: {}", experience.getExperienceId());
    }

    // 과거 체험은 수정 불가
    if (experience.getExperienceDate().isBefore(today)) {
      throw new IllegalStateException("과거 체험은 수정할 수 없습니다.");
    }

    // 진행중인 예약이 있는 경우 특정 필드 수정 제한
    List<BoothExperienceReservation> inProgressReservations = reservationRepository.findInProgressReservationsByExperience(
        experience);

    if (!inProgressReservations.isEmpty()) {
      throw new IllegalStateException("현재 진행중인 예약이 있어 수정할 수 없습니다.");
    }
  }

  // 4. 부스 체험 예약 신청 (참여자) - 동시성 처리
  @Transactional
  public BoothExperienceReservationResponseDto createReservation(Long experienceId, Long userId,
      BoothExperienceReservationRequestDto requestDto) {
    log.info("부스 체험 예약 시작 - 체험 ID: {}, 사용자 ID: {}", experienceId, userId);

    // Pessimistic Lock으로 체험 조회 (동시성 제어)
    BoothExperience experience = boothExperienceRepository.findByIdWithPessimisticLock(experienceId)
        .orElseThrow(() -> new IllegalArgumentException("체험을 찾을 수 없습니다: " + experienceId));

    Users user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

    // 예약 가능 여부 검증
    validateReservationRequest(experience, user);

    // 대기중 상태 코드 조회
    BoothExperienceStatusCode waitingStatus = statusCodeRepository.findByCode("WAITING")
        .orElseThrow(() -> new IllegalStateException("대기 상태 코드를 찾을 수 없습니다"));

    // 다음 대기 순번 조회 (동시성 처리 - experienceId 사용)
    Integer nextPosition = reservationRepository.findNextQueuePosition(experienceId);

    BoothExperienceReservation reservation = BoothExperienceReservation.builder()
        .boothExperience(experience).user(user).experienceStatusCode(waitingStatus)
        .queuePosition(nextPosition).notes(requestDto.getNotes()).build();

    BoothExperienceReservation savedReservation = reservationRepository.save(reservation);
    log.info("부스 체험 예약 완료 - 예약 ID: {}, 대기 순번: {}", savedReservation.getReservationId(),
        savedReservation.getQueuePosition());
    // 현재 내 예약의 순번 실시간 알림
    waitingNotification(userId, savedReservation.getQueuePosition(),"");
    return BoothExperienceReservationResponseDto.fromEntity(savedReservation);
  }

  // 5. 부스 체험 예약자 목록 조회 (부스 담당자용)
  @Transactional(readOnly = true)
  public List<BoothExperienceReservationResponseDto> getExperienceReservations(Long experienceId) {
    BoothExperience experience = boothExperienceRepository.findById(experienceId)
        .orElseThrow(() -> new IllegalArgumentException("체험을 찾을 수 없습니다: " + experienceId));

    List<BoothExperienceReservation> reservations = reservationRepository.findByBoothExperienceOrderByReservedAt(
        experience);

    return reservations.stream().map(BoothExperienceReservationResponseDto::fromEntity)
        .collect(Collectors.toList());
  }

  // 6. 내 부스 체험 예약 목록 조회 (참여자용)
  @Transactional(readOnly = true)
  public List<BoothExperienceReservationResponseDto> getMyReservations(Long userId) {
    Users user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

    List<BoothExperienceReservation> reservations = reservationRepository.findByUserOrderByReservedAtDesc(
        user);
    return reservations.stream().map(BoothExperienceReservationResponseDto::fromEntity)
        .collect(Collectors.toList());
  }

  // 7. 부스 체험 상태 변경 (부스 담당자)
  @Transactional
  public BoothExperienceReservationResponseDto updateReservationStatus(Long reservationId,
      BoothExperienceStatusUpdateDto updateDto) {
    log.info("예약 상태 변경 시작 - 예약 ID: {}, 새 상태: {}", reservationId, updateDto.getStatusCode());

    BoothExperienceReservation reservation = reservationRepository.findById(reservationId)
        .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));

    BoothExperienceStatusCode newStatus = statusCodeRepository.findByCode(updateDto.getStatusCode())
        .orElseThrow(
            () -> new IllegalArgumentException("유효하지 않은 상태 코드입니다: " + updateDto.getStatusCode()));

    // 상태 변경 유효성 검증
    validateStatusChange(reservation, newStatus);

    // 상태 변경 처리
    processStatusChange(reservation, newStatus, updateDto.getNotes());

    BoothExperienceReservation updatedReservation = reservationRepository.save(reservation);
    log.info("예약 상태 변경 완료 - 예약 ID: {}, 상태: {}", reservationId, newStatus.getCode());


    return BoothExperienceReservationResponseDto.fromEntity(updatedReservation);
  }

  // 부스 입장 시 예약 조회 및 상태 검증
  public BoothExperienceReservation validateReservation(BoothEntryRequestDto dto, Users user) {
    BoothExperience boothExperience = boothExperienceRepository.findById(dto.getBoothExperienceId())
        .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "부스 체험이 조회되지 않습니다"));

    log.info("boothExperience: {}", boothExperience);

    // 해당 부스 체험의 예약
    // 최근 READY 상태 예약 조회
    BoothExperienceReservation reservation = reservationRepository.findLatestReadyReservation(
        dto.getEventId(),
        dto.getBoothId(),
        user.getUserId()
    ).orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "입장 가능한 예약이 없습니다."));

    if (!reservation.getUser().getUserId().equals(user.getUserId())) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "QR 티켓 소유주와 예약자가 일치하지 않습니다.");
    }

    Event event = boothExperience.getBooth().getEvent();
    Booth booth = boothExperience.getBooth();

    if (!event.getEventId().equals(dto.getEventId())) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "선택한 행사 정보가 일치하지 않습니다.");
    }
    if (!booth.getId().equals(dto.getBoothId())) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "선택한 부스 정보가 일치하지 않습니다.");
    }
    return reservation;
  }

  // 예약 요청 유효성 검증
  private void validateReservationRequest(BoothExperience experience, Users user) {
    // 예약 활성화 여부 확인
    if (!experience.getIsReservationEnabled()) {
      throw new IllegalStateException("현재 예약이 비활성화되어 있습니다.");
    }

    // 예약 가능 여부 확인
    if (!experience.isReservationAvailable()) {
      throw new IllegalStateException("현재 예약이 불가능합니다. 대기열이 가득참.");
    }

    // 중복 예약 방지
    if (!experience.getAllowDuplicateReservation()) {
      reservationRepository.findByBoothExperienceAndUser(experience, user).ifPresent(existing -> {
        if (existing.isActive()) {
          throw new IllegalStateException("이미 예약하신 체험입니다.");
        }
      });
    }
  }

  // 상태 변경 유효성 검증
  private void validateStatusChange(BoothExperienceReservation reservation,
      BoothExperienceStatusCode newStatus) {
    String currentStatus = reservation.getExperienceStatusCode().getCode();
    String targetStatus = newStatus.getCode();

    // 상태 전환 규칙 검증
    switch (currentStatus) {
      case "WAITING":
        if (!"READY".equals(targetStatus) && !"CANCELLED".equals(targetStatus)) {
          throw new IllegalArgumentException("대기중 상태에서는 입장가능 또는 취소 상태로만 변경 가능합니다.");
        }
        break;
      case "READY":
        if (!"IN_PROGRESS".equals(targetStatus) && !"CANCELLED".equals(targetStatus)) {
          throw new IllegalArgumentException("입장가능 상태에서는 체험중 또는 취소 상태로만 변경 가능합니다.");
        }
        break;
      case "IN_PROGRESS":
        if (!"COMPLETED".equals(targetStatus)) {
          throw new IllegalArgumentException("체험중 상태에서는 완료 상태로만 변경 가능합니다.");
        }
        break;
      case "COMPLETED":
      case "CANCELLED":
        throw new IllegalArgumentException("완료 또는 취소된 예약의 상태는 변경할 수 없습니다.");
    }

    // 최대 수용 인원 확인 (IN_PROGRESS로 변경 시)
    if ("IN_PROGRESS".equals(targetStatus)) {
      BoothExperience experience = reservation.getBoothExperience();
      int currentParticipants = experience.getCurrentParticipants();
      if (currentParticipants >= experience.getMaxCapacity()) {
        throw new IllegalStateException("최대 수용 인원을 초과했습니다.");
      }
    }
  }

  // 상태 변경 처리
  private void processStatusChange(BoothExperienceReservation reservation,
      BoothExperienceStatusCode newStatus, String notes) {
    LocalDateTime now = LocalDateTime.now();
    reservation.setExperienceStatusCode(newStatus);

    if (notes != null && !notes.trim().isEmpty()) {
      reservation.setNotes(notes);
    }

    switch (newStatus.getCode()) {
      case "READY":
        reservation.setReadyAt(now);
        waitingNotification(reservation.getUser().getUserId(),reservation.getQueuePosition()-1,"입장 해주세요.");
        break;
      case "IN_PROGRESS":
        reservation.setStartedAt(now);
        waitingNotification(reservation.getUser().getUserId(), 0, "체험중입니다!");
        // 다음 대기자를 READY 상태로 변경
        // processNextWaitingReservation(reservation.getBoothExperience());
        break;
      case "COMPLETED":
        log.info("COMPLETED: {}", reservation.getExperienceStatusCode().getCode());

        // 사용자 상태 COMPLETED 변경 -> 상태 완료로 변경
        reservation.setCompletedAt(now);
        // 다음 대기자를 READY 상태로 변경
        waitingNotification(reservation.getUser().getUserId(), 0, "예약된 체험 없음");
        processNextWaitingReservation(reservation.getBoothExperience());
        break;
      case "CANCELLED":
        reservation.setCancelledAt(now);
        // 대기 순번 재정렬
        reorderQueuePositions(reservation.getBoothExperience());
        break;
    }
  }

  // 다음 대기자를 READY 상태로 변경
  private void processNextWaitingReservation(BoothExperience experience) {
    List<BoothExperienceReservation> waitingReservations = reservationRepository.findWaitingReservations(
        experience);

    if (!waitingReservations.isEmpty()) {
      BoothExperienceReservation nextReservation = waitingReservations.get(0);
      BoothExperienceStatusCode readyStatus = statusCodeRepository.findByCode("READY")
          .orElseThrow(() -> new IllegalStateException("준비 상태 코드를 찾을 수 없습니다"));

      nextReservation.setExperienceStatusCode(readyStatus);
      nextReservation.setReadyAt(LocalDateTime.now());
      waitingNotification(nextReservation.getUser().getUserId(), 0, "입장해주세요!");
      reservationRepository.save(nextReservation);
    }
  }

  // 대기 순번 재정렬 (취소 시)
  private void reorderQueuePositions(BoothExperience experience) {
    List<BoothExperienceReservation> waitingReservations = reservationRepository.findWaitingReservations(
        experience);

    for (int i = 0; i < waitingReservations.size(); i++) {
      BoothExperienceReservation reservation = waitingReservations.get(i);
      reservation.setQueuePosition(i + 1);
      // 이후 대기자들 대기 순번 재알림
      waitingNotification(reservation.getUser().getUserId(), reservation.getQueuePosition(),"");
    }
    reservationRepository.saveAll(waitingReservations);
  }

  // 사용자 본인 예약 취소 (보안 검증 포함)
  @Transactional
  public void cancelUserReservation(Long reservationId, Long userId) {
    log.info("사용자 예약 취소 시작 - 예약 ID: {}, 사용자 ID: {}", reservationId, userId);

    BoothExperienceReservation reservation = reservationRepository.findById(reservationId)
        .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));

    // 예약자 본인 확인 (보안 검증)
    if (!reservation.getUser().getUserId().equals(userId)) {
      throw new IllegalStateException("본인의 예약만 취소할 수 있습니다.");
    }

    // 이미 취소된 예약인지 확인
    if ("CANCELLED".equals(reservation.getExperienceStatusCode().getCode())) {
      throw new IllegalStateException("이미 취소된 예약입니다.");
    }

    // 완료된 예약인지 확인
    if ("COMPLETED".equals(reservation.getExperienceStatusCode().getCode())) {
      throw new IllegalStateException("완료된 예약은 취소할 수 없습니다.");
    }

    // 체험 진행중인 예약인지 확인
    if ("IN_PROGRESS".equals(reservation.getExperienceStatusCode().getCode())) {
      throw new IllegalStateException("현재 체험 진행중인 예약은 취소할 수 없습니다.");
    }

    // 체험 날짜가 지났는지 확인
    BoothExperience experience = reservation.getBoothExperience();
    if (experience.getExperienceDate().isBefore(java.time.LocalDate.now())) {
      throw new IllegalStateException("체험 날짜가 지난 예약은 취소할 수 없습니다.");
    }

    // 취소 상태로 변경
    BoothExperienceStatusCode cancelledStatus = statusCodeRepository.findByCode("CANCELLED")
        .orElseThrow(() -> new IllegalStateException("취소 상태 코드를 찾을 수 없습니다"));

    processStatusChange(reservation, cancelledStatus, "사용자 취소");
    reservationRepository.save(reservation);

    log.info("사용자 예약 취소 완료 - 예약 ID: {}", reservationId);
  }

  // 권한별 관리 가능한 부스 목록 조회 (체험 등록용)
  @Transactional(readOnly = true)
  public List<BoothResponseDto> getManageableBooths(Long userId, String roleCode) {
    log.info("권한별 관리 가능한 부스 목록 조회 - 사용자 ID: {}, 권한: {}", userId, roleCode);

    List<Booth> booths;

    if ("EVENT_MANAGER".equals(roleCode)) {
      // 행사 담당자: 해당 사용자가 관리하는 행사의 모든 부스 조회
      booths = boothRepository.findByEventManagerId(userId);
      log.info("EVENT_MANAGER - 관리 행사의 모든 부스 조회: {}개", booths.size());
    } else if ("BOOTH_MANAGER".equals(roleCode)) {
      // 부스 담당자: 해당 사용자가 관리하는 부스만 조회
      booths = boothRepository.findByBoothAdminId(userId);
      log.info("BOOTH_MANAGER - 관리 부스만 조회: {}개", booths.size());
    } else {
      // 기타 권한: 빈 목록 반환
      log.warn("권한 없음 - 빈 목록 반환, 권한: {}", roleCode);
      booths = List.of();
    }

    return booths.stream().map(BoothResponseDto::fromEntity).collect(Collectors.toList());
  }

  // 권한별 관리 가능한 체험 목록 조회 (체험 관리용)
  @Transactional(readOnly = true)
  public List<BoothExperienceResponseDto> getManageableExperiences(Long userId, String roleCode) {
    log.info("권한별 관리 가능한 체험 목록 조회 - 사용자 ID: {}, 권한: {}", userId, roleCode);

    List<BoothExperience> experiences;

    if ("EVENT_MANAGER".equals(roleCode)) {
      // 행사 담당자: 해당 사용자가 관리하는 행사의 모든 부스 체험 조회
      experiences = boothExperienceRepository.findByEventManagerId(userId);
      log.info("EVENT_MANAGER - 관리 행사의 모든 체험 조회: {}개", experiences.size());
    } else if ("BOOTH_MANAGER".equals(roleCode)) {
      // 부스 담당자: 해당 사용자가 관리하는 부스의 체험만 조회
      experiences = boothExperienceRepository.findByBoothAdminId(userId);
      log.info("BOOTH_MANAGER - 관리 부스의 체험만 조회: {}개", experiences.size());
    } else {
      // 기타 권한: 빈 목록 반환
      log.warn("권한 없음 - 빈 목록 반환, 권한: {}", roleCode);
      experiences = List.of();
    }

    return experiences.stream().map(BoothExperienceResponseDto::fromEntity)
        .collect(Collectors.toList());
  }

  // 예약자 관리용 목록 조회 (필터링 지원)
  @Transactional(readOnly = true)
  public Page<ReservationManagementResponseDto> getReservationsForManagement(Long userId,
      String roleCode, ReservationManagementRequestDto requestDto) {
    log.info("예약자 관리 목록 조회 - 사용자 ID: {}, 권한: {}", userId, roleCode);

    // 정렬 설정
    Sort sort =
        requestDto.getSortDirection().equalsIgnoreCase("desc") ? Sort.by(requestDto.getSortBy())
            .descending() : Sort.by(requestDto.getSortBy()).ascending();

    Pageable pageable = PageRequest.of(requestDto.getPage(), requestDto.getSize(), sort);

    Page<BoothExperienceReservation> reservations;

    if ("EVENT_MANAGER".equals(roleCode)) {
      // 행사 담당자: 관리하는 행사의 모든 예약 조회
      reservations = reservationRepository.findReservationsForEventManager(userId,
          requestDto.getBoothId(), requestDto.getReserverName(), requestDto.getReserverPhone(),
          requestDto.getExperienceDate(), requestDto.getStatusCode(), pageable);
    } else if ("BOOTH_MANAGER".equals(roleCode)) {
      // 부스 담당자: 관리하는 부스의 예약만 조회
      reservations = reservationRepository.findReservationsForBoothManager(userId,
          requestDto.getBoothId(), requestDto.getReserverName(), requestDto.getReserverPhone(),
          requestDto.getExperienceDate(), requestDto.getStatusCode(), pageable);
    } else {
      throw new IllegalArgumentException("권한이 없습니다.");
    }

    return reservations.map(ReservationManagementResponseDto::fromEntity);
  }

  // 부스 체험 현황 요약 조회
  @Transactional(readOnly = true)
  public BoothExperienceSummaryDto getExperienceSummary(Long experienceId) {
    log.info("부스 체험 현황 요약 조회 - 체험 ID: {}", experienceId);

    BoothExperience experience = boothExperienceRepository.findById(experienceId)
        .orElseThrow(() -> new IllegalArgumentException("체험을 찾을 수 없습니다: " + experienceId));

    // 현재 체험중인 인원 조회
    List<BoothExperienceReservation> currentParticipants = reservationRepository.findByBoothExperienceAndStatusCode(
        experience, "IN_PROGRESS");

    // 대기중인 인원 조회 (WAITING + READY)
    List<BoothExperienceReservation> waitingParticipants = reservationRepository.findWaitingAndReadyReservations(
        experience);

    // 다음 입장 예약자 (대기열 첫 번째)
    String nextParticipantName = null;
    if (!waitingParticipants.isEmpty()) {
      BoothExperienceReservation nextReservation = waitingParticipants.stream().filter(
              r -> "WAITING".equals(r.getExperienceStatusCode().getCode()) || "READY".equals(
                  r.getExperienceStatusCode().getCode()))
          .min((r1, r2) -> Integer.compare(r1.getQueuePosition(), r2.getQueuePosition()))
          .orElse(null);

      if (nextReservation != null) {
        nextParticipantName = nextReservation.getUser().getName();
      }
    }

    // 현재 체험중인 인원 이름 목록
    List<String> currentParticipantNames = currentParticipants.stream()
        .map(r -> r.getUser().getName()).collect(Collectors.toList());

    return BoothExperienceSummaryDto.builder().experienceId(experience.getExperienceId())
        .experienceTitle(experience.getTitle()).boothName(experience.getBooth().getBoothTitle())
        .maxCapacity(experience.getMaxCapacity()).currentParticipants(currentParticipants.size())
        .waitingCount(waitingParticipants.size()).currentParticipantNames(currentParticipantNames)
        .nextParticipantName(nextParticipantName).congestionRate(experience.getCongestionRate())
        .isReservationAvailable(experience.isReservationAvailable()).build();
  }

  // 예약 관리용 부스 목록 조회 (간소화된 버전)
  @Transactional(readOnly = true)
  public List<BoothResponseDto> getManageableBoothsForReservationManagement(Long userId,
      String roleCode) {
    log.info("예약 관리용 부스 목록 조회 - 사용자 ID: {}, 권한: {}", userId, roleCode);

    List<Booth> booths = List.of();

    try {
      if ("EVENT_MANAGER".equals(roleCode)) {
        // 행사 담당자: 자신이 관리하는 행사의 모든 부스 조회
        booths = boothRepository.findByEventManagerId(userId);
        log.info("EVENT_MANAGER - 조회된 부스 수: {}", booths.size());

      } else if ("BOOTH_MANAGER".equals(roleCode)) {
        // 부스 담당자: 자신이 관리하는 부스만 조회
        booths = boothRepository.findByBoothAdminId(userId);
        log.info("BOOTH_MANAGER - 조회된 부스 수: {}", booths.size());

      } else {
        log.warn("지원되지 않는 권한: {}", roleCode);
        return List.of();
      }

      if (booths.isEmpty()) {
        log.warn("조회된 부스가 없습니다. 사용자 ID: {}, 권한: {}", userId, roleCode);
        return List.of();
      }

      // 조회된 부스 정보 로깅
      log.info("조회된 부스 목록:");
      for (Booth booth : booths) {
        log.info("- 부스 ID: {}, 제목: {}, 행사: {}", booth.getId(), booth.getBoothTitle(),
            booth.getEvent() != null ? booth.getEvent().getTitleKr() : "N/A");
      }

      // DTO 변환
      List<BoothResponseDto> result = booths.stream().map(BoothResponseDto::fromEntity)
          .collect(Collectors.toList());

      log.info("변환된 DTO 수: {}", result.size());
      return result;

    } catch (Exception e) {
      log.error("부스 목록 조회 중 오류 발생 - 사용자 ID: {}, 권한: {}, 오류: {}", userId, roleCode, e.getMessage(),
          e);
      return List.of();
    }
  }

  public BoothUserRecentlyWaitingCount getUserRecentlyEventWaitingCount(
      CustomUserDetails userDetails, Long eventId) {
    Optional<BoothExperienceReservation> latest = reservationRepository.findLatestActiveReservation(
        eventId, userDetails.getUserId());
    Integer count = latest.map(reservation -> {
      String code = reservation.getExperienceStatusCode().getCode();
      return "IN_PROGRESS".equals(code) ? 0
          : "READY".equals(code) ? 0 : reservation.getQueuePosition();
    }).orElse(0);

    String eventName = latest.map(reservation -> reservation.getBoothExperience().getTitle())
        .orElse(null);

    String message = "예약된 체험 없음";
    if(latest.isPresent()) {
      String code = latest.map(reservation -> reservation.getExperienceStatusCode().getCode())
          .orElse(null);
      BoothExperienceStatusCode statusCode = boothExperienceStatusCodeRepository.findByCode(code).orElse(null);
      log.info("statusCode:{}",statusCode.getCode());
      if(statusCode.getCode().equals("READY")){
        count=0;
        message = "입장해주세요!";
      }else if(statusCode.getCode().equals("IN_PROGRESS")){
        count=0;
        message = "체험 중";
      } else if(statusCode.getCode().equals("COMPLETED") || statusCode.getCode().equals("CANCELLED")){
        count=0;
        message = "예약된 체험 없음";
      }
    }
    waitingNotification(userDetails.getUserId(), count, message);
    return BoothUserRecentlyWaitingCount.builder().waitingCount(count).eventName(eventName).message(message)
        .eventId(eventId).build();
  }

  public void waitingNotification(Long userId, Integer waitingCount, String statusMessage) {
    WaitingMessage msg = new WaitingMessage(waitingCount, statusMessage);
    messagingTemplate.convertAndSend("/topic/waiting/" + userId, msg);
  }
}