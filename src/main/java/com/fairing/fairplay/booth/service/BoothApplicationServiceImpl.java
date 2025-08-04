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
}
