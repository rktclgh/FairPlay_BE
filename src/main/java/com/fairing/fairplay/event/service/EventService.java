package com.fairing.fairplay.event.service;

import com.fairing.fairplay.booth.repository.BoothApplicationRepository;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.dto.*;
import com.fairing.fairplay.event.entity.*;
import com.fairing.fairplay.event.repository.*;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.repository.EventAdminRepository;
import com.fairing.fairplay.wishlist.repository.WishlistRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final EventQueryRepositoryImpl eventQueryRepository;
    private final EventVersionRepository eventVersionRepository;
    private final EventTicketRepository eventTicketIdRepository;
    private final BoothRepository boothRepository;
    private final WishlistRepository wishlistRepository;
    private final BoothApplicationRepository boothApplicationRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final EventScheduleRepository eventScheduleRepository;

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
    public EventResponseDto createEvent(Long adminId, EventRequestDto eventRequestDto) {

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
        savedEvent.setEventCode("EVT-" + eventCode);

        // 첫 번째 버전 생성
        log.info("첫 번째 버전 생성 for eventId: {}", savedEvent.getEventId());
        EventVersion firstVersion = eventVersionService.createEventVersion(savedEvent, adminId);
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

    // 행사 상세 생성
    @Transactional
    public EventDetailResponseDto createEventDetail(Long managerId, EventDetailRequestDto eventDetailRequestDto, Long eventId) {

        Event event = checkEventAndDetail(eventId, "create");

        // 행사 상세 생성
        EventDetail eventDetail = new EventDetail();
        log.info("eventDetail 생성: {}", eventDetail);

        Integer versionNumber = createVersion(event, managerId);

        // 제목 설정
        setTitles(event, eventDetailRequestDto);

        // 주소 설정
        Location location = createAndSaveLocation(eventDetail, eventDetailRequestDto, "create");
        eventDetail.setLocation(location);

        // 지역 코드 추출 및 설정
        findRegionCode(eventDetail, eventDetailRequestDto);

        // 행사 정보 설정
        setEventDetailInfo(eventDetail, eventDetailRequestDto);

        // 카테고리 설정
        setCategories(eventDetail, eventDetailRequestDto);

        // 외부 링크 설정
        setExternalLinks(event, eventDetailRequestDto);

        log.info("연관 관계 설정");
        eventDetail.setEvent(event);
        event.setEventDetail(eventDetail);
        eventDetailRepository.save(eventDetail);
        eventRepository.saveAndFlush(event);
        log.info("행사 상세 생성 완료");

        List<ExternalLinkResponseDto> externalLinkResponseDtos = externalLinkRepository.findByEvent(event).stream()
                .map(link -> ExternalLinkResponseDto.builder()
                        .url(link.getUrl())
                        .displayText(link.getDisplayText())
                        .build())
                .toList();

        entityManager.flush();
        entityManager.refresh(event);
        entityManager.refresh(eventDetail);

        return buildEventDetailResponseDto(event, eventDetail, externalLinkResponseDtos, versionNumber, "이벤트 상세 정보가 생성되었습니다.");
    }

    // 행사 목록 조회 (메인페이지, 검색 등) - EventDetail 정보 등록해야 보임
    @Transactional
    public EventSummaryResponseDto getEvents(
            String keyword, Integer mainCategoryId, Integer subCategoryId,
            String regionName, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        log.info("행사 목록 조회 필터");
        log.info("keyword : {}", keyword);
        log.info("mainCategoryId : {}", mainCategoryId);
        log.info("subCategoryId : {}", subCategoryId);
        log.info("regionName : {}", regionName);
        log.info("fromDate : {}", fromDate);
        log.info("toDate : {}", toDate);

        Page<EventSummaryDto> eventPage = eventQueryRepository.findEventSummariesWithFilters (
                keyword, mainCategoryId, subCategoryId, regionName, fromDate, toDate, pageable);

        log.info("행사 목록 조회 완료: {}", eventPage.getTotalElements());
        return EventSummaryResponseDto.builder()
                .message("행사 목록 조회가 완료되었습니다.")
                .events(eventPage.getContent())
                .pageable(pageable)
                .totalElements(eventPage.getTotalElements())
                .totalPages(eventPage.getTotalPages())
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
    @Transactional
    public EventDetailResponseDto getEventDetail(Long eventId) {
        Event event = checkEventAndDetail(eventId, "read");
        EventDetail eventDetail = event.getEventDetail();

        List<ExternalLinkResponseDto> externalLinkResponseDtos = externalLinkRepository.findByEvent(event).stream()
                .map(link -> ExternalLinkResponseDto.builder()
                        .url(link.getUrl())
                        .displayText(link.getDisplayText())
                        .build())
                .toList();

        int versionNumber = eventVersionRepository.findTopByEventOrderByVersionNumberDesc(event)
                .map(latestVersion -> latestVersion.getVersionNumber())
                .orElse(1);
        return buildEventDetailResponseDto(event, eventDetail, externalLinkResponseDtos, versionNumber, "이벤트 상세 정보 조회가 완료되었습니다.");
    }

    // 행사명 및 숨김 상태 업데이트
    @Transactional
    public EventResponseDto updateEvent(Long eventId, EventRequestDto eventRequestDto, Long managerId) {

        Event event = checkEventAndDetail(eventId, "update");

        if (eventRequestDto.getTitleKr() != null) {
            event.setTitleKr(eventRequestDto.getTitleKr());
        }

        if (eventRequestDto.getTitleEng() != null) {
            event.setTitleEng(eventRequestDto.getTitleEng());
        }

        if (eventRequestDto.getHidden() != null) {
            event.setHidden(eventRequestDto.getHidden());
        }

        Integer newVersion = createVersion(event, managerId);

        Event savedEvent = eventRepository.save(event);

        return EventResponseDto.builder()
                .message("행사 정보가 업데이트되었습니다.")
                .eventId(savedEvent.getEventId())
                .managerId(savedEvent.getManager() != null ? savedEvent.getManager().getUser().getUserId() : null)
                .eventCode(savedEvent.getEventCode())
                .hidden(savedEvent.getHidden())
                .version(newVersion)
                .build();
    }


    // 행사 상세 업데이트
    @Transactional
    public EventDetailResponseDto updateEventDetail(Long managerId, EventDetailRequestDto eventDetailRequestDto, Long eventId) {

        Event event = checkEventAndDetail(eventId, "update");
        EventDetail eventDetail = event.getEventDetail();

        log.info("버전 생성 for eventId: {}", eventId);
        Integer newVersion = createVersion(event, managerId);

        log.info("행사 상세 업데이트 for eventId: {}", eventId);

        // 제목 설정
        setTitles(event, eventDetailRequestDto);

        // 주소 설정
        Location location = createAndSaveLocation(eventDetail, eventDetailRequestDto, "update");
        eventDetail.setLocation(location);

        // 지역 코드 추출 및 설정
        findRegionCode(eventDetail, eventDetailRequestDto);

        // 행사 정보 설정
        setEventDetailInfo(eventDetail, eventDetailRequestDto);

        // 카테고리 설정
        setCategories(eventDetail, eventDetailRequestDto);

        // 외부 링크 설정
        setExternalLinks(event, eventDetailRequestDto);

        // 연관관계 설정
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

        entityManager.flush();
        entityManager.refresh(event);
        entityManager.refresh(eventDetail);

        return buildEventDetailResponseDto(event, eventDetail, externalLinkResponseDtos, newVersion, "이벤트 상세 정보가 업데이트되었습니다.");
    }


    // 행사 삭제 - 하위 테이블 데이터도 모두 삭제
    @Transactional
    public void deleteEvent(Long eventId) {
        log.info("행사 삭제");

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다.", null));

        boolean hasPayments = !paymentRepository.findByReservationEventEventId(eventId).isEmpty();
        boolean hasBoothPayments = boothApplicationRepository.findByEvent_EventId(eventId).stream()
                .anyMatch(app -> app.getBoothPaymentStatusCode().getId() != 1);

        if (hasPayments || hasBoothPayments) {
            if (!event.getStatusCode().getCode().equals("ENDED")) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "결제 내역이 있는 행사는 종료된 후에만 삭제할 수 있습니다.");
            }
        }

        // 1. Payment 삭제
        List<Reservation> reservations = reservationRepository.findByEvent_EventId(eventId);
        if (!reservations.isEmpty()) {
            paymentRepository.deleteAllByReservationIn(reservations);
        }

        // 2. Reservation 삭제
        reservationRepository.deleteAll(reservations);

        // 3. 나머지 엔티티 삭제
        EventDetail eventDetail = event.getEventDetail();

        if (eventDetail != null) {
            eventDetailRepository.delete(eventDetail);
        }

        if (event.getExternalLinks() != null) {
            externalLinkRepository.deleteAll(event.getExternalLinks());
        }

        eventTicketIdRepository.deleteAll(event.getEventTickets());

        boothRepository.deleteAll(event.getBooths());

        eventScheduleRepository.deleteAll(eventScheduleRepository.findByEvent_EventId(eventId));

        wishlistRepository.deleteAllByEvent(event);

        boothApplicationRepository.deleteAllByEvent(event);

        eventVersionRepository.deleteAll(event.getEventVersions());
        eventRepository.deleteById(eventId);

        log.info("행사 삭제 완료");
    }

    // 행사 강제 삭제
    @Transactional
    public void forcedDeleteEvent(Long eventId) {
        log.info("행사 강제 삭제");

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다.", null));

        // 1. Payment 삭제
        List<Reservation> reservations = reservationRepository.findByEvent_EventId(eventId);
        if (!reservations.isEmpty()) {
            paymentRepository.deleteAllByReservationIn(reservations);
        }

        // 2. Reservation 삭제
        reservationRepository.deleteAll(reservations);

        // 3. 나머지 엔티티 삭제
        EventDetail eventDetail = event.getEventDetail();

        if (eventDetail != null) {
            eventDetailRepository.delete(eventDetail);
        }

        if (event.getExternalLinks() != null) {
            externalLinkRepository.deleteAll(event.getExternalLinks());
        }

        eventTicketIdRepository.deleteAll(event.getEventTickets());

        boothRepository.deleteAll(event.getBooths());

        eventScheduleRepository.deleteAll(eventScheduleRepository.findByEvent_EventId(eventId));

        wishlistRepository.deleteAllByEvent(event);

        boothApplicationRepository.deleteAllByEvent(event);

        eventVersionRepository.deleteAll(event.getEventVersions());
        eventRepository.deleteById(eventId);

        log.info("행사 삭제 완료");
    }


    /*********************** 헬퍼 메소드 ***********************/

    private Event checkEventAndDetail(Long eventId, String mode) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다.", null));

        return switch (mode) {
            case "create" -> {
                if (event.getEventDetail() != null) {
                    throw new CustomException(HttpStatus.CONFLICT, "해당 행사의 상세 정보가 이미 존재합니다. UPDATE로 실행 해주세요.", null);
                }
                yield event;
            }
            case "update" -> {
                if (event.getEventDetail() == null) {
                    throw new CustomException(HttpStatus.BAD_REQUEST, "해당 행사의 상세 정보가 존재하지 않습니다. 상세 정보를 먼저 생성하세요.");
                }
                yield event;
            }
            case "read" -> {
                if (event.getEventDetail() == null) {
                    throw new CustomException(HttpStatus.NOT_FOUND, "해당 행사의 상세 정보가 존재하지 않습니다.");
                }
                yield event;
            }
            default -> null;
        };
    }

    private Integer createVersion(Event event, Long createdBy) {
        log.info("버전 생성 for eventId: {}", event.getEventId());
        EventVersion newVersion = eventVersionService.createEventVersion(event, createdBy);
        return newVersion.getVersionNumber();
    }

    private void setTitles(Event event, EventDetailRequestDto eventDetailRequestDto) {
        // 제목 설정
        log.info("제목 설정");
        if (eventDetailRequestDto.getTitleKr() != null) {
            event.setTitleKr(eventDetailRequestDto.getTitleKr());
        }
        if (eventDetailRequestDto.getTitleEng() != null) {
            event.setTitleEng(eventDetailRequestDto.getTitleEng());
        }
    }

    private Location createAndSaveLocation(EventDetail eventDetail, EventDetailRequestDto eventDetailRequestDto, String mode) {
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

        Location location = eventDetail.getLocation();
        if (mode.equals("create")) {
            location = new Location();
        }

        if (eventDetailRequestDto.getAddress() != null) location.setAddress(eventDetailRequestDto.getAddress());
        if (eventDetailRequestDto.getPlaceName() != null)
            location.setPlaceName(eventDetailRequestDto.getPlaceName());
        if (eventDetailRequestDto.getLatitude() != null) location.setLatitude(eventDetailRequestDto.getLatitude());
        if (eventDetailRequestDto.getLongitude() != null)
            location.setLongitude(eventDetailRequestDto.getLongitude());
        if (eventDetailRequestDto.getPlaceUrl() != null) location.setPlaceUrl(eventDetailRequestDto.getPlaceUrl());

        return locationRepository.save(location);
    }

    private void findRegionCode(EventDetail eventDetail, EventDetailRequestDto eventDetailRequestDto) {
        log.info("지역 코드 추출 및 설정");
        if (eventDetailRequestDto.getAddress() != null) {
            String regionName = eventDetailRequestDto.getAddress().substring(0, 2);
            RegionCode regionCode = regionCodeRepository.findByName(regionName)
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 지역코드를 찾지 못했습니다." + regionName, null));
            eventDetail.setRegionCode(regionCode);
        }
    }

    private void setCategories(EventDetail eventDetail, EventDetailRequestDto eventDetailRequestDto) {
        log.info("카테고리 설정");
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
    }

    private void setExternalLinks(Event event, EventDetailRequestDto eventDetailRequestDto) {
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
    }

    private void setEventDetailInfo(EventDetail eventDetail, EventDetailRequestDto eventDetailRequestDto) {
        log.info("행사 정보 설정");
        // NOT NULL : content, policy, startDate, endDate, Main&Sub Category
        if (eventDetail.getContent() == null && eventDetailRequestDto.getContent() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "content는 반드시 포함되어야 합니다.");
        } else if (eventDetailRequestDto.getContent() != null) {
            eventDetail.setContent(eventDetailRequestDto.getContent());
        }

        if (eventDetail.getPolicy() == null && eventDetailRequestDto.getPolicy() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "policy는 반드시 포함되어야 합니다.");
        } else if (eventDetailRequestDto.getPolicy() != null) {
            eventDetail.setPolicy(eventDetailRequestDto.getPolicy());
        }

        if (eventDetail.getStartDate() == null && eventDetailRequestDto.getStartDate() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "startDate는 반드시 포함되어야 합니다.");
        } else if (eventDetailRequestDto.getStartDate() != null) {
            eventDetail.setStartDate(eventDetailRequestDto.getStartDate());
        }

        if (eventDetail.getEndDate() == null && eventDetailRequestDto.getEndDate() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "endDate는 반드시 포함되어야 합니다.");
        } else if (eventDetailRequestDto.getEndDate() != null) {
            eventDetail.setEndDate(eventDetailRequestDto.getEndDate());
        }

        if (eventDetailRequestDto.getLocationDetail() != null)
            eventDetail.setLocationDetail(eventDetailRequestDto.getLocationDetail());
        if (eventDetailRequestDto.getHostName() != null) eventDetail.setHostName(eventDetailRequestDto.getHostName());
        if (eventDetailRequestDto.getContactInfo() != null)
            eventDetail.setContactInfo(eventDetailRequestDto.getContactInfo());
        if (eventDetailRequestDto.getBio() != null) eventDetail.setBio(eventDetailRequestDto.getBio());
        if (eventDetailRequestDto.getOfficialUrl() != null)
            eventDetail.setOfficialUrl(eventDetailRequestDto.getOfficialUrl());
        if (eventDetailRequestDto.getEventTime() != null)
            eventDetail.setEventTime(eventDetailRequestDto.getEventTime());
        if (eventDetailRequestDto.getThumbnailUrl() != null)
            eventDetail.setThumbnailUrl(eventDetailRequestDto.getThumbnailUrl());
        if (eventDetailRequestDto.getReentryAllowed() != null)
            eventDetail.setReentryAllowed(eventDetailRequestDto.getReentryAllowed());
        if (eventDetailRequestDto.getCheckOutAllowed() != null)
            eventDetail.setCheckOutAllowed(eventDetailRequestDto.getCheckOutAllowed());
    }

    private EventDetailResponseDto buildEventDetailResponseDto(Event event, EventDetail detail, List<ExternalLinkResponseDto> links, Integer version, String message) {
        return EventDetailResponseDto.builder()
                .message(message)
                .managerId(event.getManager().getUser().getUserId())
                .eventCode(event.getEventCode())
                .createdAt(detail.getCreatedAt())
                .updatedAt(detail.getUpdatedAt())
                .version(version)
                .titleKr(event.getTitleKr())
                .titleEng(event.getTitleEng())
                .hidden(event.getHidden())
                .eventStatusCode(event.getStatusCode() != null ? event.getStatusCode().getCode() : null)
                .mainCategory(detail.getMainCategory() != null ? detail.getMainCategory().getGroupName() : null)
                .subCategory(detail.getSubCategory() != null ? detail.getSubCategory().getCategoryName() : null)
                .address(detail.getLocation() != null ? detail.getLocation().getAddress() : null)
                .placeName(detail.getLocation() != null ? detail.getLocation().getPlaceName() : null)
                .latitude(detail.getLocation() != null ? detail.getLocation().getLatitude() : null)
                .longitude(detail.getLocation() != null ? detail.getLocation().getLongitude() : null)
                .placeUrl(detail.getLocation() != null ? detail.getLocation().getPlaceUrl() : null)
                .locationDetail(detail.getLocationDetail())
                .region(detail.getRegionCode() != null ? detail.getRegionCode().getCode() : null)
                .startDate(detail.getStartDate())
                .endDate(detail.getEndDate())
                .thumbnailUrl(detail.getThumbnailUrl())
                .hostName(detail.getHostName())
                .contactInfo(detail.getContactInfo())
                .officialUrl(detail.getOfficialUrl())
                .bio(detail.getBio())
                .content(detail.getContent())
                .policy(detail.getPolicy())
                .eventTime(detail.getEventTime())
                .externalLinks(links)
                .reentryAllowed(detail.getReentryAllowed())
                .checkOutAllowed(detail.getCheckOutAllowed())
                .build();
    }
}