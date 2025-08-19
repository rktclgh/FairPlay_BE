package com.fairing.fairplay.booth.mapper;

import com.fairing.fairplay.booth.dto.BoothApplicationListDto;
import com.fairing.fairplay.booth.dto.BoothApplicationRequestDto;
import com.fairing.fairplay.booth.dto.BoothApplicationResponseDto;
import com.fairing.fairplay.booth.dto.BoothExternalLinkDto;
import com.fairing.fairplay.booth.entity.BoothApplication;
import com.fairing.fairplay.booth.entity.BoothApplicationStatusCode;
import com.fairing.fairplay.booth.entity.BoothPaymentStatusCode;
import com.fairing.fairplay.booth.entity.BoothType;
import com.fairing.fairplay.booth.repository.BoothExternalLinkRepository;
import com.fairing.fairplay.event.entity.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BoothApplicationMapper {

    private final BoothExternalLinkRepository boothExternalLinkRepository;

    // 고객 신청 요청 DTO → 엔티티 변환
    public BoothApplication toEntity(BoothApplicationRequestDto dto,
                                     Event event,
                                     BoothType boothType,
                                     BoothApplicationStatusCode status,
                                     BoothPaymentStatusCode paymentStatus) {

        BoothApplication entity = new BoothApplication();

        entity.setEvent(event);
        entity.setBoothTitle(dto.getBoothTitle());
        entity.setBoothEmail(dto.getBoothEmail());
        entity.setBoothDescription(dto.getBoothDescription());
        entity.setManagerName(dto.getManagerName());
        entity.setContactEmail(dto.getContactEmail());
        entity.setContactNumber(dto.getContactNumber());
        entity.setBoothApplicationStatusCode(status);
        entity.setBoothPaymentStatusCode(paymentStatus);
        entity.setApplyAt(LocalDateTime.now());
        entity.setBoothType(boothType);


        // 신청 기간
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());

        return entity;
    }

    // 목록 조회용 DTO
    public BoothApplicationListDto toListDto(BoothApplication entity) {
        return BoothApplicationListDto.builder()
                .boothApplicationId(entity.getId())
                .boothTitle(entity.getBoothTitle())
                .boothTypeName(entity.getBoothType().getName())
                .price(entity.getBoothType().getPrice())
                .managerName(entity.getManagerName())
                .contactEmail(entity.getContactEmail())
                .applyAt(entity.getApplyAt())
                .statusCode(entity.getBoothApplicationStatusCode().getCode())
                .statusName(entity.getBoothApplicationStatusCode().getName())
                .paymentStatus(entity.getBoothPaymentStatusCode().getName())
                .paymentStatusCode(entity.getBoothPaymentStatusCode().getCode())
                .build();
    }

    // 상세 조회용 DTO
    public BoothApplicationResponseDto toResponseDto(BoothApplication entity) {
        return BoothApplicationResponseDto.builder()
                .boothApplicationId(entity.getId())
                .boothTitle(entity.getBoothTitle())
                .boothDescription(entity.getBoothDescription())
                .managerName(entity.getManagerName())
                .boothEmail(entity.getBoothEmail())
                .contactEmail(entity.getContactEmail())
                .contactNumber(entity.getContactNumber())
                .boothTypeName(entity.getBoothType().getName())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .boothExternalLinks(toBoothExternalLinkDto(entity))
                .boothBannerUrl(entity.getBoothBannerUrl())
                .statusCode(entity.getBoothApplicationStatusCode().getCode())
                .paymentStatus(entity.getBoothPaymentStatusCode().getCode())
                .applyAt(entity.getApplyAt())
                .adminComment(entity.getAdminComment())
                .build();
    }

    public List<BoothExternalLinkDto> toBoothExternalLinkDto(BoothApplication entity) {
        return boothExternalLinkRepository.findByBoothApplication(entity).stream()
                .map(link -> BoothExternalLinkDto.builder()
                        .url(link.getUrl())
                        .displayText(link.getDisplayText())
                        .build()).toList();
    }
}
