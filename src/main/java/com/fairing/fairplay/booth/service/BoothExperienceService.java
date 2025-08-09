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
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // 1. 부스 체험 등록 (부스 담당자)
    @Transactional
    public BoothExperienceResponseDto createBoothExperience(Long boothId, BoothExperienceRequestDto requestDto) {
        log.info("부스 체험 등록 시작 - 부스 ID: {}", boothId);

        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new IllegalArgumentException("부스를 찾을 수 없습니다: " + boothId));

        BoothExperience experience = BoothExperience.builder()
                .booth(booth)
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .experienceDate(requestDto.getExperienceDate())
                .startTime(requestDto.getStartTime())
                .endTime(requestDto.getEndTime())
                .durationMinutes(requestDto.getDurationMinutes())
                .maxCapacity(requestDto.getMaxCapacity())
                .allowWaiting(requestDto.getAllowWaiting() != null ? requestDto.getAllowWaiting() : true)
                .maxWaitingCount(requestDto.getMaxWaitingCount())
                .allowDuplicateReservation(requestDto.getAllowDuplicateReservation() != null ? 
                        requestDto.getAllowDuplicateReservation() : false)
                .isReservationEnabled(requestDto.getIsReservationEnabled() != null ? 
                        requestDto.getIsReservationEnabled() : true)
                .build();

        BoothExperience savedExperience = boothExperienceRepository.save(experience);
        log.info("부스 체험 등록 완료 - 체험 ID: {}", savedExperience.getExperienceId());

        return BoothExperienceResponseDto.fromEntity(savedExperience);
    }

    // 2. 부스 체험 목록 조회 (부스 담당자용)
    @Transactional(readOnly = true)
    public List<BoothExperienceResponseDto> getBoothExperiences(Long boothId) {
        List<BoothExperience> experiences = boothExperienceRepository.findByBooth_Id(boothId);
        return experiences.stream()
                .map(BoothExperienceResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 3. 체험 가능한 부스 체험 목록 조회 (참여자용)
    @Transactional(readOnly = true)
    public List<BoothExperienceResponseDto> getAvailableExperiences() {
        LocalDate today = LocalDate.now();
        List<BoothExperience> experiences = boothExperienceRepository.findAvailableExperiences(today);
        return experiences.stream()
                .map(BoothExperienceResponseDto::fromEntity)
                .collect(Collectors.toList());
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
                .boothExperience(experience)
                .user(user)
                .experienceStatusCode(waitingStatus)
                .queuePosition(nextPosition)
                .notes(requestDto.getNotes())
                .build();

        BoothExperienceReservation savedReservation = reservationRepository.save(reservation);
        log.info("부스 체험 예약 완료 - 예약 ID: {}, 대기 순번: {}", 
                savedReservation.getReservationId(), savedReservation.getQueuePosition());

        return BoothExperienceReservationResponseDto.fromEntity(savedReservation);
    }

    // 5. 부스 체험 예약자 목록 조회 (부스 담당자용)
    @Transactional(readOnly = true)
    public List<BoothExperienceReservationResponseDto> getExperienceReservations(Long experienceId) {
        BoothExperience experience = boothExperienceRepository.findById(experienceId)
                .orElseThrow(() -> new IllegalArgumentException("체험을 찾을 수 없습니다: " + experienceId));

        List<BoothExperienceReservation> reservations = 
                reservationRepository.findByBoothExperienceOrderByReservedAt(experience);

        return reservations.stream()
                .map(BoothExperienceReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 6. 내 부스 체험 예약 목록 조회 (참여자용)
    @Transactional(readOnly = true)
    public List<BoothExperienceReservationResponseDto> getMyReservations(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        List<BoothExperienceReservation> reservations = reservationRepository.findByUserOrderByReservedAtDesc(user);
        return reservations.stream()
                .map(BoothExperienceReservationResponseDto::fromEntity)
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
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 상태 코드입니다: " + updateDto.getStatusCode()));

        // 상태 변경 유효성 검증
        validateStatusChange(reservation, newStatus);

        // 상태 변경 처리
        processStatusChange(reservation, newStatus, updateDto.getNotes());

        BoothExperienceReservation updatedReservation = reservationRepository.save(reservation);
        log.info("예약 상태 변경 완료 - 예약 ID: {}, 상태: {}", reservationId, newStatus.getCode());

        return BoothExperienceReservationResponseDto.fromEntity(updatedReservation);
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
            reservationRepository.findByBoothExperienceAndUser(experience, user)
                    .ifPresent(existing -> {
                        if (existing.isActive()) {
                            throw new IllegalStateException("이미 예약하신 체험입니다.");
                        }
                    });
        }
    }

    // 상태 변경 유효성 검증
    private void validateStatusChange(BoothExperienceReservation reservation, BoothExperienceStatusCode newStatus) {
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
                break;
            case "IN_PROGRESS":
                reservation.setStartedAt(now);
                // 다음 대기자를 READY 상태로 변경
                processNextWaitingReservation(reservation.getBoothExperience());
                break;
            case "COMPLETED":
                reservation.setCompletedAt(now);
                // 다음 대기자를 READY 상태로 변경
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
        List<BoothExperienceReservation> waitingReservations = 
                reservationRepository.findWaitingReservations(experience);

        if (!waitingReservations.isEmpty()) {
            BoothExperienceReservation nextReservation = waitingReservations.get(0);
            BoothExperienceStatusCode readyStatus = statusCodeRepository.findByCode("READY")
                    .orElseThrow(() -> new IllegalStateException("준비 상태 코드를 찾을 수 없습니다"));

            nextReservation.setExperienceStatusCode(readyStatus);
            nextReservation.setReadyAt(LocalDateTime.now());
            reservationRepository.save(nextReservation);
            
            log.info("다음 대기자 호출 - 예약 ID: {}", nextReservation.getReservationId());
        }
    }

    // 대기 순번 재정렬 (취소 시)
    private void reorderQueuePositions(BoothExperience experience) {
        List<BoothExperienceReservation> waitingReservations = 
                reservationRepository.findWaitingReservations(experience);

        for (int i = 0; i < waitingReservations.size(); i++) {
            BoothExperienceReservation reservation = waitingReservations.get(i);
            reservation.setQueuePosition(i + 1);
        }

        reservationRepository.saveAll(waitingReservations);
        log.info("대기 순번 재정렬 완료 - 체험 ID: {}", experience.getExperienceId());
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
}