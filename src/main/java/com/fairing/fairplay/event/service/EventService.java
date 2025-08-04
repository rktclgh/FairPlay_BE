package com.fairing.fairplay.event.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.dto.*;
import com.fairing.fairplay.event.entity.*;
import com.fairing.fairplay.event.repository.*;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.repository.EventAdminRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashids.Hashids;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final Hashids hashids;
    private final EventRepository eventRepository;
    private final EventStatusCodeRepository eventStatusCodeRepository;
    private final EventAdminRepository eventAdminRepository;
    private final EventVersionService eventVersionService;
    private final ExternalLinkRepository externalLinkRepository;
    private final EventDetailRepository eventDetailRepository;
    private final LocationRepository locationRepository;
    private final RegionCodeRepository regionCodeRepository;
    private final MainCategoryRepository mainCategoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    @PersistenceContext
    private EntityManager entityManager;


    // Hashids 인코딩/디코딩
    public String encode(Long id) {
        return hashids.encode(id);
    }

    public Long decode(String hash) {
        long[] decoded = hashids.decode(hash);
        if (decoded.length == 0) return null;
        return decoded[0];
    }

    /*
     *  행사 생성
     *      1. 전체관리자가 행사 승인
     *      2. EventRequestDto에서 입력받은 email로 행사 관리자 계정 생성
     *      3. 생성된 행사 관리자 계정의 사용자 id를 managerId로 설정
     *      4. 행사 고유 코드(evnetCode) 생성
     *          - 슬러그 + Hashid
     *      5. version 1로 설정
     */

    // 전체 관리자가 구독 생성
    @Transactional
    public EventResponseDto createEvent(EventRequestDto eventRequestDto) {

        log.info("행사 관리자 계정 생성 시작");
        // TODO: 행사 관리자 계정 생성 및 ID 받기
        Long managerId = 2L;    // NOTE: 임시로 하드코딩
        log.info("행사 관리자 계정 생성 완료");

        log.info("행사 생성 시작");
        Event event = new Event();

        // 행사 담당자 설정
        EventAdmin manager = eventAdminRepository.findById(managerId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 행사 관리자를 찾을 수 없습니다.", null));
        event.setManager(manager);
        log.info("담당자 설정 완료");

        // 행사 상태 UPCOMING으로 설정
        EventStatusCode status = eventStatusCodeRepository.findById(1)
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "해당 코드 없음", null));
        event.setStatusCode(status);
        log.info("행사 상태 설정 완료");

        // 행사 생성 후 행사 ID로 고유 코드 생성하기 위해 임시값 저장
        event.setEventCode("TEMP");
        log.info("행사 고유 코드 임시 설정 완료");

        // 행사명 설정
        event.setTitleKr(eventRequestDto.getTitleKr());
        event.setTitleEng(eventRequestDto.getTitleEng());
        log.info("행사명 생성 완료");

        // Hashid로 고유 코드 생성
        Event savedEvent = eventRepository.save(event);
        String eventCode = encode(savedEvent.getEventId());
        savedEvent.setEventCode(eventCode);

        // 첫 번째 버전 생성
        log.info("첫 번째 버전 생성 for eventId: {}", savedEvent.getEventId());
        EventVersion firstVersion = eventVersionService.createEventVersion(savedEvent, 1L); // TODO: 전체 관리자 id 받아오는 걸로 수정하기
        log.info("첫 번째 버전 생성 완료");

        return EventResponseDto.builder()
                .message("행사 생성이 완료되었습니다.")
                .eventId(savedEvent.getEventId())
                .managerId(manager.getUser().getUserId())
                .eventCode(eventCode)
                .hidden(event.getHidden())
                .version(firstVersion.getVersionNumber())
                .build();
    }

    @Transactional
    public EventDetailResponseDto createEventDetail(EventDetailRequestDto eventDetailRequestDto, Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 이벤트가 존재하지 않습니다", null));

        Long managerId = event.getManager().getUser().getUserId(); // TODO: 로그인한 담당자 ID로 변경

        if (event.getEventDetail() != null) {
            throw new CustomException(HttpStatus.CONFLICT, "해당 이벤트의 상세 정보가 이미 존재합니다. UPDATE로 실행 해주세요.", null);
        }

        // 행사 상세 생성
        EventDetail eventDetail = new EventDetail();
        log.info("eventDetail 생성: {}", eventDetail);

        log.info("버전 생성 for eventId: {}", eventId);
        EventVersion newVersion = eventVersionService.createEventVersion(event, managerId);

        // 제목 설정
        log.info("제목 설정");
        if (eventDetailRequestDto.getTitleKr() != null) {
            event.setTitleKr(eventDetailRequestDto.getTitleKr());
        }
        if (eventDetailRequestDto.getTitleEng() != null) {
            event.setTitleEng(eventDetailRequestDto.getTitleEng());
        }

        // 주소 설정
        log.info("주소 설정");
        // TODO: 주소값(도로명, 건물명, 위도, 경도)은 프론트에서 axios로 넘겨줘야함
        /*
                프론트에서 할일 - 카카오맵 API
                1. 사용자가 키워드로 장소 검색
                2. 사용자가 장소 선택 및 상세 주소 입력
                    선택 된 장소에서 가져올 데이터
                    - place_name
                    - road_address_name
                    - x
                    - y
                    - place_url
        */

        Location location = new Location();
        location.setAddress(eventDetailRequestDto.getAddress());
        location.setPlaceName(eventDetailRequestDto.getPlaceName());
        location.setLatitude(eventDetailRequestDto.getLatitude());
        location.setLongitude(eventDetailRequestDto.getLongitude());
        location.setPlaceUrl(eventDetailRequestDto.getPlaceUrl());

        eventDetail.setLocation(location);
        locationRepository.save(location);

        if (eventDetailRequestDto.getLocationDetail() != null) {
            eventDetail.setLocationDetail(eventDetailRequestDto.getLocationDetail());
        }

        // 지역 코드 추출 및 설정
        String regionName = eventDetailRequestDto.getAddress().substring(0, 2);
        RegionCode regionCode = regionCodeRepository.findByName(regionName)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 지역코드를 찾지 못했습니다.", null));
        eventDetail.setRegionCode(regionCode);

        // 행사 정보 설정
        log.info("행사 정보 설정");
        eventDetail.setHostName(eventDetailRequestDto.getHostName());
        eventDetail.setContactInfo(eventDetailRequestDto.getContactInfo());
        eventDetail.setBio(eventDetailRequestDto.getBio());
        eventDetail.setContent(eventDetailRequestDto.getContent());
        eventDetail.setPolicy(eventDetailRequestDto.getPolicy());
        eventDetail.setOfficialUrl(eventDetailRequestDto.getOfficialUrl());
        eventDetail.setEventTime(eventDetailRequestDto.getEventTime());
        eventDetail.setThumbnailUrl(eventDetailRequestDto.getThumbnailUrl());
        eventDetail.setStartDate(eventDetailRequestDto.getStartDate());
        eventDetail.setEndDate(eventDetailRequestDto.getEndDate());
        eventDetail.setReentryAllowed(eventDetailRequestDto.getReentryAllowed());
        eventDetail.setCheckOutAllowed(eventDetailRequestDto.getCheckOutAllowed());

        // 카테고리 설정
        log.info("카테고리 설정");
        MainCategory mainCategory = mainCategoryRepository.findById(eventDetailRequestDto.getMainCategoryId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 메인 카테고리를 찾지 못했습니다.", null));
        eventDetail.setMainCategory(mainCategory);

        SubCategory subCategory = subCategoryRepository.findById(eventDetailRequestDto.getSubCategoryId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 서브 카테고리를 찾지 못했습니다.", null));
        eventDetail.setSubCategory(subCategory);

        // 외부 링크 설정
        log.info("외부 링크 설정");
        if (eventDetailRequestDto.getExternalLinks() != null) {
            Set<ExternalLink> links = eventDetailRequestDto.getExternalLinks().stream()
                    .map(linkDto -> {
                        ExternalLink externalLink = new ExternalLink();
                        externalLink.setEvent(event);
                        externalLink.setUrl(linkDto.getUrl());
                        externalLink.setDisplayText(linkDto.getDisplayText());
                        return externalLink;
                    })
                    .collect(Collectors.toSet());

            event.getExternalLinks().clear();
            event.getExternalLinks().addAll(links);
        }

        log.info("연관 관계 설정");
        eventDetail.setEvent(event);
        event.setEventDetail(eventDetail);
        log.info("event 저장 직전");
        eventRepository.saveAndFlush(event);
        log.info("행사 상세 생성 완료");
        eventDetailRepository.flush();

        List<ExternalLinkResponseDto> externalLinkResponseDtos = externalLinkRepository.findByEvent(event).stream()
                .map(link -> ExternalLinkResponseDto.builder()
                        .url(link.getUrl())
                        .displayText(link.getDisplayText())
                        .build())
                .toList();

        entityManager.clear();

        Event updatedEvent = eventRepository.findById(event.getEventId())
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "event 재조회 실패", null));
        EventDetail updatedDetail = eventDetailRepository.findById(event.getEventId())
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "eventDetail 재조회 실패", null));

        return EventDetailResponseDto.builder()
                .message("이벤트 상세 정보가 생성되었습니다.")
                .managerId(updatedEvent.getManager().getUser().getUserId())
                .eventCode(updatedEvent.getEventCode())
                .createdAt(updatedDetail.getCreatedAt())
                .updatedAt(updatedDetail.getUpdatedAt())
                .version(newVersion.getVersionNumber())
                .titleKr(updatedEvent.getTitleKr())
                .titleEng(updatedEvent.getTitleEng())
                .hidden(updatedEvent.getHidden())
                .eventStatusCode(updatedEvent.getStatusCode() != null ? updatedEvent.getStatusCode().getCode() : null)
                .mainCategory(updatedDetail.getMainCategory() != null ? updatedDetail.getMainCategory().getGroupName() : null)
                .subCategory(updatedDetail.getSubCategory() != null ? updatedDetail.getSubCategory().getCategoryName() : null)
                .address(updatedDetail.getLocation() != null ? updatedDetail.getLocation().getAddress() : null)
                .placeName(updatedDetail.getLocation() != null ? updatedDetail.getLocation().getPlaceName() : null)
                .latitude(updatedDetail.getLocation() != null ? updatedDetail.getLocation().getLatitude() : null)
                .longitude(updatedDetail.getLocation() != null ? updatedDetail.getLocation().getLongitude() : null)
                .placeUrl(updatedDetail.getLocation() != null ? updatedDetail.getLocation().getPlaceUrl() : null)
                .locationDetail(updatedDetail.getLocationDetail())
                .region(updatedDetail.getRegionCode() != null ? updatedDetail.getRegionCode().getCode() : null)
                .startDate(updatedDetail.getStartDate())
                .endDate(updatedDetail.getEndDate())
                .thumbnailUrl(updatedDetail.getThumbnailUrl())
                .hostName(updatedDetail.getHostName())
                .contactInfo(updatedDetail.getContactInfo())
                .officialUrl(updatedDetail.getOfficialUrl())
                .bio(updatedDetail.getBio())
                .content(updatedDetail.getContent())
                .policy(updatedDetail.getPolicy())
                .eventTime(updatedDetail.getEventTime())
                .externalLinks(externalLinkResponseDtos)
                .reentryAllowed(updatedDetail.getReentryAllowed())
                .checkOutAllowed(updatedDetail.getCheckOutAllowed())
                .build();
    }

    // 행사 목록 조회 (메인페이지, 검색 등) - EventDetail 정보 등록해야 보임
    @Transactional(readOnly = true)
    public EventSummaryResponseDto getEvents(Pageable pageable) {
        Page<Event> eventsPage = eventRepository.findByHiddenFalseAndEventDetailIsNotNull(pageable);
        List<EventSummaryDto> eventSummaries = eventsPage.getContent().stream()
                .map(event -> EventSummaryDto.builder()
                        .id(event.getEventId())
                        .eventCode(event.getEventCode())
                        .hidden(event.getHidden())
                        .title(event.getTitleKr())
                        .minPrice(event.getEventTickets().stream()
                                .map(eventTicket -> eventTicket.getTicket().getPrice())
                                .min(java.util.Comparator.naturalOrder())
                                .orElse(null))
                        .mainCategory(event.getEventDetail() != null && event.getEventDetail().getMainCategory() != null ? event.getEventDetail().getMainCategory().getGroupName() : null)
                        .thumbnailUrl(event.getEventDetail() != null ? event.getEventDetail().getThumbnailUrl() : null)
                        .startDate(event.getEventDetail() != null ? event.getEventDetail().getStartDate() : null)
                        .endDate(event.getEventDetail() != null ? event.getEventDetail().getEndDate() : null)
                        .region(event.getEventDetail() != null && event.getEventDetail().getRegionCode() != null ? event.getEventDetail().getRegionCode().getCode() : null)
                        .location(event.getEventDetail() != null && event.getEventDetail().getLocation() != null ? event.getEventDetail().getLocation().getPlaceName() : null)
                        .hidden(event.getHidden())
                        .build())
                .toList();

        return EventSummaryResponseDto.builder()
                .message("행사 목록 조회가 완료되었습니다.")
                .events(eventSummaries)
                .pageable(pageable)
                .totalElements(eventsPage.getTotalElements())
                .totalPages(eventsPage.getTotalPages())
                .build();
    }

    // 행사 목록 조회 (테스트용)
    @Transactional(readOnly = true)
    public List<EventResponseDto> getEventList() {
        List<Event> events = eventRepository.findAll();
        return events.stream()
                .map(event -> EventResponseDto.builder()
                        .eventId(event.getEventId())
                        .managerId(event.getManager() != null ? event.getManager().getUser().getUserId() : null)
                        .eventCode(event.getEventCode())
                        .hidden(event.getHidden())
                        .version(event.getEventVersions() != null && !event.getEventVersions().isEmpty() ? event.getEventVersions().stream().mapToInt(EventVersion::getVersionNumber).max().orElse(0) : 0)
                        .build())
                .toList();
    }

    // 행사 상세 조회
    @Transactional(readOnly = true)
    public EventDetailResponseDto getEventDetail(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 이벤트를 찾을 수 없습니다.", null));

        EventDetail eventDetail = event.getEventDetail();
        if (eventDetail == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "해당 이벤트의 상세 정보가 없습니다.", null);
        }

        List<ExternalLinkResponseDto> externalLinkResponseDtos = externalLinkRepository.findByEvent(event).stream()
                .map(link -> ExternalLinkResponseDto.builder()
                        .url(link.getUrl())
                        .displayText(link.getDisplayText())
                        .build())
                .toList();

        return EventDetailResponseDto.builder()
                .message("이벤트 상세 정보 조회가 완료되었습니다.")
                .managerId(event.getManager() != null ? event.getManager().getUser().getUserId() : null)
                .eventCode(event.getEventCode())
                .createdAt(eventDetail.getCreatedAt())
                .updatedAt(eventDetail.getUpdatedAt())
                .version(event.getEventVersions() != null && !event.getEventVersions().isEmpty() ? event.getEventVersions().stream().mapToInt(EventVersion::getVersionNumber).max().orElse(0) : 0)
                .titleKr(event.getTitleKr())
                .titleEng(event.getTitleEng())
                .hidden(event.getHidden())
                .eventStatusCode(event.getStatusCode() != null ? event.getStatusCode().getCode() : null)
                .mainCategory(eventDetail.getMainCategory() != null ? eventDetail.getMainCategory().getGroupName() : null)
                .subCategory(eventDetail.getSubCategory() != null ? eventDetail.getSubCategory().getCategoryName() : null)
                .address(eventDetail.getLocation() != null ? eventDetail.getLocation().getAddress() : null)
                .placeName(eventDetail.getLocation() != null ? eventDetail.getLocation().getPlaceName() : null)
                .latitude(eventDetail.getLocation() != null ? eventDetail.getLocation().getLatitude() : null)
                .longitude(eventDetail.getLocation() != null ? eventDetail.getLocation().getLongitude() : null)
                .placeUrl(eventDetail.getLocation() != null ? eventDetail.getLocation().getPlaceUrl() : null)
                .locationDetail(eventDetail.getLocationDetail())
                .region(eventDetail.getRegionCode() != null ? eventDetail.getRegionCode().getCode() : null)
                .startDate(eventDetail.getStartDate())
                .endDate(eventDetail.getEndDate())
                .thumbnailUrl(eventDetail.getThumbnailUrl())
                .hostName(eventDetail.getHostName())
                .contactInfo(eventDetail.getContactInfo())
                .officialUrl(eventDetail.getOfficialUrl())
                .bio(eventDetail.getBio())
                .content(eventDetail.getContent())
                .policy(eventDetail.getPolicy())
                .eventTime(eventDetail.getEventTime())
                .externalLinks(externalLinkResponseDtos)
                .reentryAllowed(eventDetail.getReentryAllowed())
                .checkOutAllowed(eventDetail.getCheckOutAllowed())
                .build();
    }

    // 행사명 및 숨김 상태 업데이트
    @Transactional
    public EventResponseDto updateEvent(Long eventId, EventRequestDto eventRequestDto, Long managerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다.", null));

        if (eventRequestDto.getTitleKr() != null) {
            event.setTitleKr(eventRequestDto.getTitleKr());
        }

        if (eventRequestDto.getTitleEng() != null) {
            event.setTitleEng(eventRequestDto.getTitleEng());
        }

        if (eventRequestDto.getHidden() != null) {
            event.setHidden(eventRequestDto.getHidden());
        }

        EventVersion newVersion = eventVersionService.createEventVersion(event, managerId);

        Event savedEvent = eventRepository.save(event);

        return EventResponseDto.builder()
                .message("행사 정보가 업데이트되었습니다.")
                .eventId(savedEvent.getEventId())
                .managerId(savedEvent.getManager() != null ? savedEvent.getManager().getUser().getUserId() : null)
                .eventCode(savedEvent.getEventCode())
                .hidden(savedEvent.getHidden())
                .version(newVersion.getVersionNumber())
                .build();
    }


    // 행사 상세 업데이트
    @Transactional
    public EventDetailResponseDto updateEventDetail(EventDetailRequestDto eventDetailRequestDto, Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 이벤트가 존재하지 않습니다", null));

        EventDetail eventDetail = event.getEventDetail();
        if (eventDetail == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "해당 이벤트의 상세 정보가 없습니다. 생성을 먼저 해주세요.", null);
        }

        Long managerId = event.getManager().getUser().getUserId(); // TODO: 로그인한 담당자 ID로 변경

        log.info("버전 생성 for eventId: {}", eventId);
        EventVersion newVersion = eventVersionService.createEventVersion(event, managerId);

        log.info("행사 상세 업데이트 for eventId: {}", eventId);

        // 제목 설정
        log.info("제목 설정");
        if (eventDetailRequestDto.getTitleKr() != null) {
            event.setTitleKr(eventDetailRequestDto.getTitleKr());
        }
        if (eventDetailRequestDto.getTitleEng() != null) {
            event.setTitleKr(eventDetailRequestDto.getTitleEng());
        }

        // TODO: 주소값(도로명, 건물명, 위도, 경도)은 프론트에서 axios로 넘겨줘야함
        /*
                프론트에서 할일 - 카카오맵 API
                1. 사용자가 키워드로 장소 검색
                2. 사용자가 장소 선택 및 상세 주소 입력
                    선택 된 장소에서 가져올 데이터
                    - place_name
                    - road_address_name
                    - x
                    - y
                    - place_url
        */
        log.info("주소 설정");
        if (eventDetailRequestDto.getAddress() != null) {
            Location location = eventDetail.getLocation();
            if (location == null) location = new Location();

            if (eventDetailRequestDto.getAddress() != null) location.setAddress(eventDetailRequestDto.getAddress());
            if (eventDetailRequestDto.getPlaceName() != null)
                location.setPlaceName(eventDetailRequestDto.getPlaceName());
            if (eventDetailRequestDto.getLatitude() != null) location.setLatitude(eventDetailRequestDto.getLatitude());
            if (eventDetailRequestDto.getLongitude() != null)
                location.setLongitude(eventDetailRequestDto.getLongitude());
            if (eventDetailRequestDto.getPlaceUrl() != null) location.setPlaceUrl(eventDetailRequestDto.getPlaceUrl());

            eventDetail.setLocation(location);
            locationRepository.save(location);

            String regionName = eventDetailRequestDto.getAddress().substring(0, 2);
            RegionCode regionCode = regionCodeRepository.findByName(regionName)
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 지역코드를 찾지 못했습니다.", null));
            eventDetail.setRegionCode(regionCode);
        }

        log.info("행사 정보 설정");
        if (eventDetailRequestDto.getLocationDetail() != null)
            eventDetail.setLocationDetail(eventDetailRequestDto.getLocationDetail());
        if (eventDetailRequestDto.getHostName() != null) eventDetail.setHostName(eventDetailRequestDto.getHostName());
        if (eventDetailRequestDto.getContactInfo() != null)
            eventDetail.setContactInfo(eventDetailRequestDto.getContactInfo());
        if (eventDetailRequestDto.getBio() != null) eventDetail.setBio(eventDetailRequestDto.getBio());
        if (eventDetailRequestDto.getContent() != null) eventDetail.setContent(eventDetailRequestDto.getContent());
        if (eventDetailRequestDto.getPolicy() != null) eventDetail.setPolicy(eventDetailRequestDto.getPolicy());
        if (eventDetailRequestDto.getOfficialUrl() != null)
            eventDetail.setOfficialUrl(eventDetailRequestDto.getOfficialUrl());
        if (eventDetailRequestDto.getEventTime() != null)
            eventDetail.setEventTime(eventDetailRequestDto.getEventTime());
        if (eventDetailRequestDto.getThumbnailUrl() != null)
            eventDetail.setThumbnailUrl(eventDetailRequestDto.getThumbnailUrl());
        if (eventDetailRequestDto.getStartDate() != null)
            eventDetail.setStartDate(eventDetailRequestDto.getStartDate());
        if (eventDetailRequestDto.getEndDate() != null) eventDetail.setEndDate(eventDetailRequestDto.getEndDate());
        if (eventDetailRequestDto.getReentryAllowed() != null)
            eventDetail.setReentryAllowed(eventDetailRequestDto.getReentryAllowed());
        if (eventDetailRequestDto.getCheckOutAllowed() != null)
            eventDetail.setCheckOutAllowed(eventDetailRequestDto.getCheckOutAllowed());

        // 카테고리 설정
        if (eventDetailRequestDto.getMainCategoryId() != null) {
            MainCategory mainCategory = mainCategoryRepository.findById(eventDetailRequestDto.getMainCategoryId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 메인 카테고리를 찾지 못했습니다.", null));
            eventDetail.setMainCategory(mainCategory);
        }

        if (eventDetailRequestDto.getSubCategoryId() != null) {
            SubCategory subCategory = subCategoryRepository.findById(eventDetailRequestDto.getSubCategoryId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 서브 카테고리를 찾지 못했습니다.", null));
            eventDetail.setSubCategory(subCategory);
        }

        // 외부 링크 설정
        log.info("외부 링크 설정");
        if (eventDetailRequestDto.getExternalLinks() != null) {
            List<ExternalLink> existingLinks = externalLinkRepository.findByEvent(event);
            List<ExternalLinkRequestDto> newLinkDtos = eventDetailRequestDto.getExternalLinks();

            Set<String> newLinkUrls = new HashSet<>();
            for (ExternalLinkRequestDto dto : newLinkDtos) {
                newLinkUrls.add(dto.getUrl());
            }

            List<ExternalLink> toDelete = new ArrayList<>();
            for (ExternalLink existingLink : existingLinks) {
                if (!newLinkUrls.contains(existingLink.getUrl())) {
                    toDelete.add(existingLink);
                }
            }
            externalLinkRepository.deleteAll(toDelete);

            List<ExternalLink> toAddOrUpdate = new ArrayList<>();
            for (ExternalLinkRequestDto dto : newLinkDtos) {
                ExternalLink link = existingLinks.stream()
                        .filter(l -> l.getUrl().equals(dto.getUrl()))
                        .findFirst()
                        .orElse(new ExternalLink());

                link.setEvent(event);
                link.setUrl(dto.getUrl());
                link.setDisplayText(dto.getDisplayText());
                toAddOrUpdate.add(link);
            }
            externalLinkRepository.saveAll(toAddOrUpdate);
        }

        eventDetail.setEvent(event);
        event.setEventDetail(eventDetail);
        eventRepository.save(event);
        eventDetailRepository.flush();

        log.info("행사 상세 업데이트 완료: {}", eventDetail.getEventDetailId());

        List<ExternalLinkResponseDto> externalLinkResponseDtos = externalLinkRepository.findByEvent(event).stream()
                .map(link -> ExternalLinkResponseDto.builder()
                        .url(link.getUrl())
                        .displayText(link.getDisplayText())
                        .build())
                .toList();

        entityManager.clear();

        Event updatedEvent = eventRepository.findById(event.getEventId())
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "event 재조회 실패", null));
        EventDetail updatedDetail = eventDetailRepository.findById(event.getEventId())
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "eventDetail 재조회 실패", null));

        return EventDetailResponseDto.builder()
                .message("이벤트 상세 정보가 업데이트되었습니다.")
                .managerId(updatedEvent.getManager().getUser().getUserId())
                .eventCode(updatedEvent.getEventCode())
                .createdAt(updatedDetail.getCreatedAt())
                .updatedAt(updatedDetail.getUpdatedAt())
                .version(newVersion.getVersionNumber())
                .titleKr(updatedEvent.getTitleKr())
                .titleEng(updatedEvent.getTitleEng())
                .hidden(updatedEvent.getHidden())
                .eventStatusCode(updatedEvent.getStatusCode() != null ? updatedEvent.getStatusCode().getCode() : null)
                .mainCategory(updatedDetail.getMainCategory() != null ? updatedDetail.getMainCategory().getGroupName() : null)
                .subCategory(updatedDetail.getSubCategory() != null ? updatedDetail.getSubCategory().getCategoryName() : null)
                .address(updatedDetail.getLocation() != null ? updatedDetail.getLocation().getAddress() : null)
                .placeName(updatedDetail.getLocation() != null ? updatedDetail.getLocation().getPlaceName() : null)
                .latitude(updatedDetail.getLocation() != null ? updatedDetail.getLocation().getLatitude() : null)
                .longitude(updatedDetail.getLocation() != null ? updatedDetail.getLocation().getLongitude() : null)
                .placeUrl(updatedDetail.getLocation() != null ? updatedDetail.getLocation().getPlaceUrl() : null)
                .locationDetail(updatedDetail.getLocationDetail())
                .region(updatedDetail.getRegionCode() != null ? updatedDetail.getRegionCode().getCode() : null)
                .startDate(updatedDetail.getStartDate())
                .endDate(updatedDetail.getEndDate())
                .thumbnailUrl(updatedDetail.getThumbnailUrl())
                .hostName(updatedDetail.getHostName())
                .contactInfo(updatedDetail.getContactInfo())
                .officialUrl(updatedDetail.getOfficialUrl())
                .bio(updatedDetail.getBio())
                .content(updatedDetail.getContent())
                .policy(updatedDetail.getPolicy())
                .eventTime(updatedDetail.getEventTime())
                .externalLinks(externalLinkResponseDtos)
                .reentryAllowed(updatedDetail.getReentryAllowed())
                .checkOutAllowed(updatedDetail.getCheckOutAllowed())
                .build();
    }


}