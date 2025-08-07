package com.fairing.fairplay.booth.mapper;

import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.entity.*;
import com.fairing.fairplay.event.entity.Event;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BoothApplicationMapper {

    // 고객 신청 요청 DTO → 엔티티 변환
    public BoothApplication toEntity(BoothApplicationRequestDto dto,
                                     Event event,
                                     BoothType boothType,
                                     BoothApplicationStatusCode status,
                                     BoothPaymentStatusCode paymentStatus) {

        BoothApplication entity = new BoothApplication();

        entity.setEvent(event);
        entity.setBoothTitle(dto.getBoothTitle());
        entity.setBoothEmail(dto.getEmail());
        entity.setBoothDescription(dto.getBoothDescription());
        entity.setManagerName(dto.getManagerName());
        entity.setEmail(dto.getEmail());
        entity.setContactNumber(dto.getContactNumber());
        entity.setOfficialUrl(dto.getOfficialUrl());
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
                .id(entity.getId())
                .boothTitle(entity.getBoothTitle())
                .boothTypeName(entity.getBoothType().getName())
                .applyAt(entity.getApplyAt())
                .statusCode(entity.getBoothApplicationStatusCode().getCode())
                .paymentStatus(entity.getBoothPaymentStatusCode().getCode())
                .build();
    }

    // 상세 조회용 DTO
    public BoothApplicationResponseDto toResponseDto(BoothApplication entity) {
        return BoothApplicationResponseDto.builder()
                .id(entity.getId())
                .eventTitle(entity.getEvent().getTitleKr())
                .boothTitle(entity.getBoothTitle())
                .boothDescription(entity.getBoothDescription())
                .managerName(entity.getManagerName())
                .email(entity.getEmail())
                .contactNumber(entity.getContactNumber())
                .officialUrl(entity.getOfficialUrl())
                .boothTypeName(entity.getBoothType().getName())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .statusCode(entity.getBoothApplicationStatusCode().getCode())
                .paymentStatus(entity.getBoothPaymentStatusCode().getCode())
                .applyAt(entity.getApplyAt())
                .adminComment(entity.getAdminComment())
                .build();
    }
}
