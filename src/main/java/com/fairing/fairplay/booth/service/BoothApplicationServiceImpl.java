package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.entity.*;
import com.fairing.fairplay.booth.repository.BoothApplicationRepository;
import com.fairing.fairplay.booth.repository.BoothApplicationStatusCodeRepository;
import com.fairing.fairplay.booth.repository.BoothPaymentStatusCodeRepository;
import com.fairing.fairplay.booth.mapper.BoothApplicationMapper;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoothApplicationServiceImpl implements BoothApplicationService {

    private final BoothApplicationRepository boothApplicationRepository;
    private final EventRepository eventRepository;
    private final BoothApplicationStatusCodeRepository statusCodeRepository;
    private final BoothPaymentStatusCodeRepository paymentCodeRepository;
    private final BoothApplicationMapper mapper;

    @Override
    public Long applyBooth(BoothApplicationRequestDto dto) {

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다.");
        }

        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("행사를 찾을 수 없습니다."));

        BoothApplicationStatusCode status = statusCodeRepository.findByCode("PENDING")
                .orElseThrow(() -> new EntityNotFoundException("상태 코드 없음"));

        BoothPaymentStatusCode paymentStatus = paymentCodeRepository.findByCode("PENDING")
                .orElseThrow(() -> new EntityNotFoundException("결제 상태 코드 없음"));

        BoothApplication entity = mapper.toEntity(dto, event, status, paymentStatus);
        BoothApplication saved = boothApplicationRepository.save(entity);

        return saved.getId();
    }

    @Override
    public List<BoothApplicationListDto> getBoothApplications(Long eventId) {
        return boothApplicationRepository.findByEvent_EventId(eventId).stream()
                .map(mapper::toListDto)
                .toList();
    }

        @Override
    public BoothApplicationResponseDto getBoothApplication(Long id) {
        BoothApplication application = boothApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("신청 정보를 찾을 수 없습니다."));
        return mapper.toResponseDto(application);
    }

    @Override
    public void updateStatus(Long id, BoothApplicationStatusUpdateDto dto) {
        BoothApplication application = boothApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("신청 정보를 찾을 수 없습니다."));

        BoothApplicationStatusCode newStatus = statusCodeRepository.findByCode(dto.getStatusCode())
                .orElseThrow(() -> new EntityNotFoundException("상태 코드가 유효하지 않습니다."));

        application.setBoothApplicationStatusCode(newStatus);
        application.setAdminComment(dto.getAdminComment());
        application.setStatusUpdatedAt(LocalDateTime.now());
    }

    @Override
    public void updatePaymentStatus(Long id, BoothPaymentStatusUpdateDto dto) {
        BoothApplication booth = boothApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("부스 신청 정보를 찾을 수 없습니다."));

        BoothPaymentStatusCode statusCode = paymentCodeRepository
                .findByCode(dto.getPaymentStatusCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 결제 상태 코드입니다."));

        booth.setBoothPaymentStatusCode(statusCode);
        booth.setAdminComment(dto.getAdminComment());  // 관리자 사유 기록
        booth.setStatusUpdatedAt(LocalDateTime.now()); // 상태 변경 시간 기록

    }

    /*
    // 여기 추가
    @Override
    public void cancelApplication(Long id, Long userId) {
        BoothApplication application = boothApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("부스 신청 정보를 찾을 수 없습니다."));

        // 유저 이메일 가져오기 (CustomUserDetails 사용)
        String requesterEmail = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."))
                .getEmail();

        if (!application.getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new SecurityException("해당 신청을 취소할 권한이 없습니다.");
        }

        // 결제 상태를 CANCELLED로 변경
        BoothPaymentStatusCode cancelled = paymentCodeRepository.findByCode("CANCELLED")
                .orElseThrow(() -> new EntityNotFoundException("결제 상태 코드(CANCELLED)를 찾을 수 없습니다."));

        application.setBoothPaymentStatusCode(cancelled);
        application.setStatusUpdatedAt(LocalDateTime.now());
    }*/
}
