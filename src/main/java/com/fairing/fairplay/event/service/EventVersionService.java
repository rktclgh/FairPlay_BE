package com.fairing.fairplay.event.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.dto.EventDetailResponseDto;
import com.fairing.fairplay.event.dto.EventSnapshotDto;
import com.fairing.fairplay.event.dto.EventVersionComparisonDto;
import com.fairing.fairplay.event.dto.ExternalLinkResponseDto;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.event.entity.EventVersion;
import com.fairing.fairplay.event.entity.ExternalLink;
import com.fairing.fairplay.event.repository.*;
import com.fairing.fairplay.ticket.dto.TicketSnapshotDto;
import com.fairing.fairplay.ticket.entity.EventTicket;
import com.fairing.fairplay.ticket.entity.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventVersionService {

    private final EventVersionRepository eventVersionRepository;
    private final EventRepository eventRepository;
    private final EventDetailRepository eventDetailRepository;
    private final LocationRepository locationRepository;
    private final MainCategoryRepository mainCategoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final RegionCodeRepository regionCodeRepository;

    @Transactional
    public EventVersion createEventVersion(Event event, Long updatedBy) {

        try { // 행사 버전 생성
            int nextVersionNumber = eventVersionRepository.findTopByEventOrderByVersionNumberDesc(event)
                    .map(latestVersion -> latestVersion.getVersionNumber() + 1)
                    .orElse(1);

            EventSnapshotDto snapshotDto = createSnapshotDto(event);

            EventVersion eventVersion = new EventVersion();
            eventVersion.setEvent(event);
            eventVersion.setVersionNumber(nextVersionNumber);
            eventVersion.setSnapshotFromDto(snapshotDto);
            eventVersion.setUpdatedBy(updatedBy);
            eventVersion.setUpdatedAt(LocalDateTime.now());

            return eventVersionRepository.save(eventVersion);
        } catch (DataIntegrityViolationException e) {
            log.error("중복 버전 번호로 인한 제약 조건 위반: " + e.getMessage());
            throw new CustomException(HttpStatus.CONFLICT, "동시 요청으로 인해 버전 생성에 실패했습니다. 다시 시도해주세요.", e);
        }

    }

    private EventSnapshotDto createSnapshotDto(Event event) {
        if (event == null) {
            return null;
        }

        EventDetail detail = event.getEventDetail();
        EventSnapshotDto.EventSnapshotDtoBuilder builder = EventSnapshotDto.builder();

        // from Event
        builder.eventCode(event.getEventCode())
                .titleKr(event.getTitleKr())
                .titleEng(event.getTitleEng())
                .hidden(event.getHidden())
                .managerId(Optional.ofNullable(event.getManager()).map(m -> m.getUser().getUserId()).orElse(null))
                .eventStatusCodeId(Optional.ofNullable(event.getStatusCode()).map(s -> s.getEventStatusCodeId()).orElse(null));

        // tickets from Event
        Hibernate.initialize(event.getEventTickets());
        List<Ticket> tickets = Optional.ofNullable(event.getEventTickets()).orElse(Collections.emptySet()).stream()
                .map(EventTicket::getTicket)
                .toList();
        List<TicketSnapshotDto> ticketSnapshots = tickets.stream()
                .map(this::createTicketSnapshotDto)
                .toList();
        builder.tickets(ticketSnapshots);

        // from EventDetail (존재하는 경우)
        if (detail != null) {
            builder.locationId(Optional.ofNullable(detail.getLocation()).map(l -> l.getLocationId()).orElse(null))
                    .locationDetail(detail.getLocationDetail())
                    .hostName(detail.getHostName())
                    .contactInfo(detail.getContactInfo())
                    .bio(detail.getBio())
                    .content(detail.getContent())
                    .policy(detail.getPolicy())
                    .officialUrl(detail.getOfficialUrl())
                    .eventTime(detail.getEventTime())
                    .thumbnailUrl(detail.getThumbnailUrl())
                    .bannerUrl(detail.getBannerUrl())
                    .startDate(detail.getStartDate())
                    .endDate(detail.getEndDate())
                    .reentryAllowed(detail.getReentryAllowed())
                    .checkInAllowed(detail.getCheckInAllowed())
                    .checkOutAllowed(detail.getCheckOutAllowed())
                    .hostCompany(detail.getHostCompany())
                    .age(detail.getAge())
                    .mainCategoryId(Optional.ofNullable(detail.getMainCategory()).map(mc -> mc.getGroupId()).orElse(null))
                    .mainCategoryName(Optional.ofNullable(detail.getMainCategory()).map(mc -> mc.getGroupName()).orElse(null))
                    .subCategoryId(Optional.ofNullable(detail.getSubCategory()).map(sc -> sc.getCategoryId()).orElse(null))
                    .subCategoryName(Optional.ofNullable(detail.getSubCategory()).map(sc -> sc.getCategoryName()).orElse(null))
                    .regionCodeId(Optional.ofNullable(detail.getRegionCode()).map(rc -> rc.getRegionCodeId()).orElse(null));

            // from Location
            Optional.ofNullable(detail.getLocation()).ifPresent(location -> {
                builder.placeName(location.getPlaceName());
                builder.latitude(location.getLatitude());
                builder.longitude(location.getLongitude());
                builder.address(location.getAddress());
                builder.placeUrl(location.getPlaceUrl());
            });

            // external links from EventDetail
            Hibernate.initialize(event.getExternalLinks());
            List<ExternalLink> externalLinks = Optional.ofNullable(event.getExternalLinks()).orElse(Collections.emptySet()).stream().toList();
            builder.externalLinks(externalLinks.stream()
                    .map(link -> new EventSnapshotDto.ExternalLinkSnapshot(link.getUrl(), link.getDisplayText()))
                    .toList());
        } else {
            builder.externalLinks(Collections.emptyList());
        }

        // from Manager
        Optional.ofNullable(event.getManager()).ifPresent(manager -> {
            builder.businessNumber(manager.getBusinessNumber());
            builder.managerEmail(manager.getContactEmail()); // EventAdmin의 contactEmail 사용
            builder.managerPhone(manager.getContactNumber()); // EventAdmin의 contactNumber 사용
            Optional.ofNullable(manager.getUser()).ifPresent(user -> {
                builder.managerName(user.getName());
            });
        });

        return builder.build();
    }

    private TicketSnapshotDto createTicketSnapshotDto(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        return TicketSnapshotDto.builder()
                .name(ticket.getName())
                .price(ticket.getPrice())
                .stock(ticket.getStock())
                .build();
    }

    // 특정 행사의 버전 목록 조회
    @Transactional(readOnly = true)
    public Page<EventVersion> getEventVersions(Long eventId, Pageable pageable) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."));
        
        return eventVersionRepository.findByEventOrderByVersionNumberDesc(event, pageable);
    }

    // 특정 버전 상세 조회
    @Transactional(readOnly = true)
    public EventVersion getEventVersion(Long eventId, Integer versionNumber) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."));

        return eventVersionRepository.findByEventAndVersionNumber(event, versionNumber)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 버전을 찾을 수 없습니다."));
    }

    // 특정 버전으로 복구 (내부적으로만 사용 - 수정 요청 승인 시)
    @Transactional
    public void restoreToVersionInternal(Long eventId, Integer targetVersionNumber, Long restoredBy) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."));

        EventVersion targetVersion = eventVersionRepository.findByEventAndVersionNumber(event, targetVersionNumber)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "복구할 버전을 찾을 수 없습니다."));

        // 현재 최신 버전 확인
        EventVersion latestVersion = eventVersionRepository.findTopByEventOrderByVersionNumberDesc(event)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "현재 버전 정보를 찾을 수 없습니다."));

        if (latestVersion.getVersionNumber().equals(targetVersionNumber)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미 최신 버전입니다.");
        }

        // 스냅샷으로부터 데이터 복구
        EventSnapshotDto snapshotDto = targetVersion.getSnapshotAsDto();
        applySnapshotToEvent(event, snapshotDto);

        // 새로운 버전 생성 (복구한 내용으로)
        createEventVersion(event, restoredBy);

        log.info("행사 ID {} 를 버전 {} 로 복구했습니다. 복구자: {}", eventId, targetVersionNumber, restoredBy);
    }

    // 두 버전 간의 차이점 비교
    @Transactional(readOnly = true)
    public EventVersionComparisonDto compareVersions(Long eventId, Integer version1, Integer version2) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."));

        EventVersion eventVersion1 = eventVersionRepository.findByEventAndVersionNumber(event, version1)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "버전 " + version1 + "을 찾을 수 없습니다."));

        EventVersion eventVersion2 = eventVersionRepository.findByEventAndVersionNumber(event, version2)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "버전 " + version2 + "를 찾을 수 없습니다."));

        EventSnapshotDto snapshot1 = eventVersion1.getSnapshotAsDto();
        EventSnapshotDto snapshot2 = eventVersion2.getSnapshotAsDto();

        Object jsonDifferences = calculateJsonDifferences(snapshot1, snapshot2);

        return EventVersionComparisonDto.builder()
                .eventId(eventId)
                .version1(version1)
                .version2(version2)
                .snapshot1(snapshot1)
                .snapshot2(snapshot2)
                .fieldDifferences(jsonDifferences)
                .build();
    }

    // EventVersion Repository를 통한 새 버전 생성 (기존 메소드와 구분)
    @Transactional
    public void createNewVersion(Long eventId, Long updatedBy) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."));
        
        createEventVersion(event, updatedBy);
    }

    private void applySnapshotToEvent(Event event, EventSnapshotDto snapshot) {
        // Event 기본 정보 복구
        event.setTitleKr(snapshot.getTitleKr());
        event.setTitleEng(snapshot.getTitleEng());
        event.setHidden(snapshot.isHidden());

        // EventDetail 복구
        EventDetail eventDetail = event.getEventDetail();
        if (eventDetail != null && snapshot != null) {
            eventDetail.setLocationDetail(snapshot.getLocationDetail());
            eventDetail.setHostName(snapshot.getHostName());
            eventDetail.setContactInfo(snapshot.getContactInfo());
            eventDetail.setBio(snapshot.getBio());
            eventDetail.setContent(snapshot.getContent());
            eventDetail.setPolicy(snapshot.getPolicy());
            eventDetail.setOfficialUrl(snapshot.getOfficialUrl());
            eventDetail.setEventTime(snapshot.getEventTime());
            eventDetail.setThumbnailUrl(snapshot.getThumbnailUrl());
            eventDetail.setBannerUrl(snapshot.getBannerUrl());
            eventDetail.setStartDate(snapshot.getStartDate());
            eventDetail.setEndDate(snapshot.getEndDate());
            eventDetail.setReentryAllowed(snapshot.getReentryAllowed());
            eventDetail.setCheckInAllowed(snapshot.getCheckInAllowed());
            eventDetail.setCheckOutAllowed(snapshot.getCheckOutAllowed());
            eventDetail.setHostCompany(snapshot.getHostCompany());
            eventDetail.setAge(snapshot.getAge());

            // Location 설정
            if (snapshot.getLocationId() != null) {
                locationRepository.findById(snapshot.getLocationId())
                        .ifPresent(eventDetail::setLocation);
            }

            // MainCategory 설정
            if (snapshot.getMainCategoryId() != null) {
                mainCategoryRepository.findById(snapshot.getMainCategoryId())
                        .ifPresent(eventDetail::setMainCategory);
            }

            // SubCategory 설정
            if (snapshot.getSubCategoryId() != null) {
                subCategoryRepository.findById(snapshot.getSubCategoryId())
                        .ifPresent(eventDetail::setSubCategory);
            }

            // RegionCode 설정
            if (snapshot.getRegionCodeId() != null) {
                regionCodeRepository.findById(snapshot.getRegionCodeId())
                        .ifPresent(eventDetail::setRegionCode);
            }

            eventDetailRepository.save(eventDetail);
        }

        eventRepository.save(event);
    }


    private Object calculateJsonDifferences(EventSnapshotDto snapshot1, EventSnapshotDto snapshot2) {
        java.util.Map<String, Object> jsonDiff = new java.util.HashMap<>();
        
        // 변경된 필드만 포함
        addFieldIfChanged(jsonDiff, "titleKr", "제목(한국어)", snapshot1.getTitleKr(), snapshot2.getTitleKr());
        addFieldIfChanged(jsonDiff, "titleEng", "제목(영어)", snapshot1.getTitleEng(), snapshot2.getTitleEng());
        addFieldIfChanged(jsonDiff, "hidden", "숨김 상태", snapshot1.isHidden(), snapshot2.isHidden());
        addFieldIfChanged(jsonDiff, "hostName", "주최자명", snapshot1.getHostName(), snapshot2.getHostName());
        addFieldIfChanged(jsonDiff, "contactInfo", "연락처", snapshot1.getContactInfo(), snapshot2.getContactInfo());
        addFieldIfChanged(jsonDiff, "bio", "행사 소개", snapshot1.getBio(), snapshot2.getBio());
        addFieldIfChanged(jsonDiff, "content", "행사 내용", snapshot1.getContent(), snapshot2.getContent());
        addFieldIfChanged(jsonDiff, "policy", "주의사항", snapshot1.getPolicy(), snapshot2.getPolicy());
        addFieldIfChanged(jsonDiff, "officialUrl", "공식 URL", snapshot1.getOfficialUrl(), snapshot2.getOfficialUrl());
        addFieldIfChanged(jsonDiff, "eventTime", "행사 시간", snapshot1.getEventTime(), snapshot2.getEventTime());
        addFieldIfChanged(jsonDiff, "startDate", "시작일", snapshot1.getStartDate(), snapshot2.getStartDate());
        addFieldIfChanged(jsonDiff, "endDate", "종료일", snapshot1.getEndDate(), snapshot2.getEndDate());
        addFieldIfChanged(jsonDiff, "thumbnailUrl", "썸네일", snapshot1.getThumbnailUrl(), snapshot2.getThumbnailUrl());
        addFieldIfChanged(jsonDiff, "bannerUrl", "배너", snapshot1.getBannerUrl(), snapshot2.getBannerUrl());
        addFieldIfChanged(jsonDiff, "locationDetail", "상세 주소", snapshot1.getLocationDetail(), snapshot2.getLocationDetail());
        addFieldIfChanged(jsonDiff, "hostCompany", "주최 회사", snapshot1.getHostCompany(), snapshot2.getHostCompany());
        addFieldIfChanged(jsonDiff, "age", "연령 제한", snapshot1.getAge(), snapshot2.getAge());
        addFieldIfChanged(jsonDiff, "reentryAllowed", "재입장 허용", snapshot1.getReentryAllowed(), snapshot2.getReentryAllowed());
        addFieldIfChanged(jsonDiff, "checkInAllowed", "체크인 허용", snapshot1.getCheckInAllowed(), snapshot2.getCheckInAllowed());
        addFieldIfChanged(jsonDiff, "checkOutAllowed", "중간 퇴장 허용", snapshot1.getCheckOutAllowed(), snapshot2.getCheckOutAllowed());
        
        return jsonDiff;
    }

    private void addFieldIfChanged(java.util.Map<String, Object> jsonDiff, String fieldName, String displayName, Object oldValue, Object newValue) {
        if (!java.util.Objects.equals(oldValue, newValue)) {
            java.util.Map<String, Object> fieldChange = new java.util.HashMap<>();
            fieldChange.put("displayName", displayName);
            fieldChange.put("oldValue", oldValue);
            fieldChange.put("newValue", newValue);
            fieldChange.put("changeType", oldValue == null ? "added" : newValue == null ? "removed" : "modified");
            jsonDiff.put(fieldName, fieldChange);
        }
    }

    // 특정 버전을 EventDetailResponseDto 형태로 반환
    @Transactional(readOnly = true)
    public EventDetailResponseDto getEventVersionAsDetailResponse(Long eventId, Integer versionNumber) {
        EventVersion eventVersion = getEventVersion(eventId, versionNumber);
        EventSnapshotDto snapshot = eventVersion.getSnapshotAsDto();
        Event event = eventVersion.getEvent();
        
        // region 정보 조회
        String regionCode = null;
        if (snapshot.getRegionCodeId() != null) {
            regionCode = regionCodeRepository.findById(snapshot.getRegionCodeId())
                    .map(rc -> rc.getCode())
                    .orElse("");
        }
        
        return EventDetailResponseDto.builder()
                .message("버전 " + versionNumber + " 상세 정보 조회가 완료되었습니다.")
                .managerId(snapshot.getManagerId())
                .eventCode(snapshot.getEventCode())
                .createdAt(eventVersion.getUpdatedAt()) // 버전 생성 시간 사용
                .updatedAt(eventVersion.getUpdatedAt()) // 버전 생성 시간 사용
                .version(versionNumber)
                .viewCount(event.getViewCount()) // 현재 행사의 조회수 사용
                .titleKr(snapshot.getTitleKr())
                .titleEng(snapshot.getTitleEng())
                .hidden(snapshot.isHidden())
                .eventStatusCode("ACTIVE") // Default status for version display
                .mainCategory(snapshot.getMainCategoryName())
                .subCategory(snapshot.getSubCategoryName())
                .address(snapshot.getAddress())
                .placeName(snapshot.getPlaceName())
                .latitude(snapshot.getLatitude())
                .longitude(snapshot.getLongitude())
                .placeUrl(snapshot.getPlaceUrl())
                .locationDetail(snapshot.getLocationDetail())
                .region(regionCode != null ? regionCode : "")
                .startDate(snapshot.getStartDate())
                .endDate(snapshot.getEndDate())
                .thumbnailUrl(snapshot.getThumbnailUrl())
                .hostName(snapshot.getHostName())
                .hostCompany(snapshot.getHostCompany())
                .contactInfo(snapshot.getContactInfo())
                .officialUrl(snapshot.getOfficialUrl())
                .managerName(snapshot.getManagerName())
                .managerPhone(snapshot.getManagerPhone())
                .managerEmail(snapshot.getManagerEmail())
                .managerBusinessNumber(snapshot.getBusinessNumber())
                .bio(snapshot.getBio())
                .content(snapshot.getContent())
                .policy(snapshot.getPolicy())
                .eventTime(snapshot.getEventTime())
                .checkInAllowed(snapshot.getCheckInAllowed())
                .checkOutAllowed(snapshot.getCheckOutAllowed())
                .reentryAllowed(snapshot.getReentryAllowed())
                .age(snapshot.getAge())
                .externalLinks(snapshot.getExternalLinks().stream()
                        .map(link -> ExternalLinkResponseDto.builder()
                                .url(link.getUrl())
                                .displayText(link.getDisplayText())
                                .build())
                        .toList())
                .build();
    }
}