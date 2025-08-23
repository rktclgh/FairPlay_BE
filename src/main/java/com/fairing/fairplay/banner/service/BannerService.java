package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.*;
import com.fairing.fairplay.banner.entity.*;
import com.fairing.fairplay.banner.repository.*;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.file.dto.S3UploadRequestDto;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.service.FileService;
import com.fairing.fairplay.user.entity.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_PRIORITY_CHANGE = "PRIORITY_CHANGE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final BannerRepository bannerRepository;
    private final BannerStatusCodeRepository bannerStatusCodeRepository;
    private final BannerActionCodeRepository bannerActionCodeRepository;
    private final BannerLogRepository bannerLogRepository;
    private final FileService fileService;
    private final AwsS3Service awsS3Service;
    private final BannerTypeRepository bannerTypeRepository;
    private final EventRepository eventRepository;
    private final BannerSlotRepository bannerSlotRepository;


    @Value("${cloud.aws.s3.banner-dir:banner}")
    private String bannerDir;

    // 등록
    @Transactional
    public BannerResponseDto createBanner(BannerRequestDto dto, Long adminId) {
        validateEvent(dto.getEventId());


        if (dto.getStartDate() == null || dto.getEndDate() == null) {
                        throw new CustomException(HttpStatus.BAD_REQUEST, "노출 기간(startDate, endDate)은 필수입니다.", null);
                    }
                if (dto.getStartDate().isAfter(dto.getEndDate())) {
                        throw new CustomException(HttpStatus.BAD_REQUEST, "노출 기간이 올바르지 않습니다.", null);
                    }


        if ("HERO".equals(getBannerTypeOr404(dto.getBannerTypeId()).getCode())) {
            boolean dup = bannerRepository.existsByBannerType_CodeAndEventIdAndBannerStatusCode_Code("HERO", dto.getEventId(), "ACTIVE");
            if (dup) throw new CustomException(HttpStatus.CONFLICT, "해당 행사에 이미 활성 HERO 배너가 있습니다.", null);
        }

        BannerStatusCode statusCode = getStatusCodeOr404(dto.getStatusCode());
        BannerType bannerType = getBannerTypeOr404(dto.getBannerTypeId());

        Banner banner = new Banner(
                dto.getTitle(),
                null, // 이미지 URL은 파일 처리 후 설정
                dto.getLinkUrl(),
                dto.getPriority(),
                dto.getStartDate(),
                dto.getEndDate(),
                statusCode,
                bannerType
        );

        banner.setEventId(dto.getEventId());
        banner.setCreatedBy(adminId);

        Banner saved = bannerRepository.save(banner);

        String finalImageUrl = resolveImageUrlForCreate(dto, saved.getId());
        saved.setImageUrl(finalImageUrl);

        logBannerAction(saved, adminId, ACTION_CREATE);
        return toDto(saved);
    }

    // 수정
    @Transactional
    public BannerResponseDto updateBanner(Long bannerId, BannerRequestDto dto, Long adminId) {
        Banner banner = getBannerOr404(bannerId);

        if (dto.getEventId() != null) {
            validateEvent(dto.getEventId());
            banner.setEventId(dto.getEventId());
        }

        // 부분 수정 시 기존 값과 병합하여 기간 검증
                LocalDateTime newStart = dto.getStartDate() != null ? dto.getStartDate() : banner.getStartDate();
                LocalDateTime newEnd   = dto.getEndDate()   != null ? dto.getEndDate()   : banner.getEndDate();
                if (newStart.isAfter(newEnd)) {
                        throw new CustomException(HttpStatus.BAD_REQUEST, "노출 기간이 올바르지 않습니다.", null);
                    }

        BannerType newType = (dto.getBannerTypeId() != null)
                ? getBannerTypeOr404(dto.getBannerTypeId())
                : banner.getBannerType();
        String typeCode = newType.getCode();

        // HERO 변경/유지 시 중복 ACTIVE 검사 (자기 자신 제외)
               if (TYPE_HERO.equals(typeCode) && STATUS_ACTIVE.equals(banner.getBannerStatusCode().getCode())) {
                       boolean dupOther = bannerRepository
                                      .existsByBannerType_CodeAndEventIdAndBannerStatusCode_CodeAndIdNot(
                                              TYPE_HERO, banner.getEventId(), STATUS_ACTIVE, banner.getId());
                       if (dupOther) {
                               throw new CustomException(HttpStatus.CONFLICT, "해당 행사에 이미 활성 HERO 배너가 있습니다.", null);
                           }
                   }

        // HERO는 우선순위 무시(또는 금지)
        Integer priorityToApply = banner.getPriority(); // 기본은 기존 값 유지
        if (!"HERO".equals(typeCode) && dto.getPriority() != null) {
            priorityToApply = dto.getPriority();
        } else if ("HERO".equals(typeCode) && dto.getPriority() != null
                && !dto.getPriority().equals(banner.getPriority())) {
            // 원하면 경고 로그만 남기고 무시해도 됨
            throw new CustomException(HttpStatus.FORBIDDEN, "HERO 배너는 우선순위를 수정할 수 없습니다.", null);
        }

        // 통일된 이미지 처리(수정 전용: 입력 없으면 기존 유지)
        String finalImageUrl = resolveImageUrlForUpdate(dto, banner, adminId);



        banner.updateInfo(
                dto.getTitle(),
                finalImageUrl,
                dto.getLinkUrl(),
                newStart,
                newEnd,
                priorityToApply,
                newType
        );

        logBannerAction(banner, adminId, ACTION_UPDATE);
        return toDto(banner);
    }

    // 상태 우선 순위
    @Transactional
    public void changeStatus(Long bannerId, BannerStatusUpdateDto dto, Long adminId) {
        Banner banner = getBannerOr404(bannerId);
        BannerStatusCode statusCode = getStatusCodeOr404(dto.getStatusCode());
        banner.updateStatus(statusCode);
        logBannerAction(banner, adminId, ACTION_UPDATE);
    }

    @Transactional
    public void changePriority(Long bannerId, BannerPriorityUpdateDto dto, Long adminId) {
        Banner banner = getBannerOr404(bannerId);

        // HERO는 수동 변경 금지
        String type = banner.getBannerType().getCode();
        if ("HERO".equals(type)) {   // 또는 Set.of("HERO","SEARCH_TOP").contains(type)
            throw new CustomException(HttpStatus.FORBIDDEN,
                    "HERO 배너는 우선순위를 수동 변경할 수 없습니다.", null);
        }

        banner.updatePriority(dto.getPriority());
        logBannerAction(banner, adminId, ACTION_PRIORITY_CHANGE);
    }

    // 조회
    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllActiveBanners() {
        LocalDateTime now = LocalDateTime.now();
        return bannerRepository
                .findAllByBannerStatusCode_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityAsc(
                        STATUS_ACTIVE, now, now
                )
                .stream()
                .map(this::toDto)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<BannerResponseDto> getAllBanners() {
        return bannerRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // 공통 핼퍼

    private void validateEvent(Long eventId) {
        if (eventId == null || !eventRepository.existsById(eventId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다.", null);
        }
    }

    // 등록: s3Key 또는 imageUrl 반드시 필요(없으면 400)
    private String resolveImageUrlForCreate(BannerRequestDto dto, Long bannerId) {
        if (!StringUtils.hasText(dto.getS3Key()) && !StringUtils.hasText(dto.getImageUrl())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미지 정보가 없습니다. s3Key 또는 imageUrl이 필요합니다.", null);
        }
        if (StringUtils.hasText(dto.getS3Key())) {
            return uploadToS3(dto, bannerId);
        }
        return dto.getImageUrl();
    }

    // 수정: s3Key가 있으면 업로드, imageUrl이 있으면 교체, 둘 다 없으면 기존 유지
    private String resolveImageUrlForUpdate(BannerRequestDto dto, Banner banner, Long adminId) {
        if (StringUtils.hasText(dto.getS3Key())) {
            // 기존 파일이 있다면 삭제
            if (StringUtils.hasText(banner.getImageUrl())) {
                try {
                    String s3Key = awsS3Service.getS3KeyFromPublicUrl(banner.getImageUrl());
                    if (s3Key != null) {
                        fileService.deleteFileByS3Key(s3Key);
                    }
                } catch (Exception e) {
                    log.warn("기존 배너 이미지 S3 삭제 실패 - URL: {}, Error: {}", banner.getImageUrl(), e.getMessage());
                }
            }
            return uploadToS3(dto, banner.getId());
        }
        if (StringUtils.hasText(dto.getImageUrl())) {
            return dto.getImageUrl();
        }
        return banner.getImageUrl();
    }

    private String uploadToS3(BannerRequestDto dto, Long bannerId) {
        File savedFile = fileService.uploadFile(
                S3UploadRequestDto.builder()
                        .s3Key(dto.getS3Key())
                        .originalFileName(dto.getOriginalFileName())
                        .fileType(dto.getFileType())
                        .fileSize(dto.getFileSize())
                        .directoryPrefix(bannerDir)
                        .usage("banner")
                        .build()
        );
        fileService.createFileLink(savedFile, "BANNER", bannerId);
        return awsS3Service.getCdnUrl(savedFile.getFileUrl());
    }

    private void logBannerAction(Banner banner, Long adminId, String actionCodeStr) {
        BannerActionCode actionCode = bannerActionCodeRepository.findByCode(actionCodeStr)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "존재하지 않는 배너 액션 코드: " + actionCodeStr, null));

        Users proxyUser = new Users(adminId);
        BannerLog log = BannerLog.builder()
                .banner(banner)
                .changedBy(proxyUser)
                .actionCode(actionCode)
                .build();

        bannerLogRepository.save(log);
    }

    // 추가
    /** 신청 시 슬롯 LOCK (동시성 보장) */
    @Transactional
    public LockSlotsResponseDto lockSlots(LockSlotsRequestDto req, Long userId) {
        if (req == null || req.items() == null || req.items().isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "잠글 슬롯이 없습니다.", null);
        }
        var type = bannerTypeRepository.findByCode(req.typeCode())
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "배너 타입(code) 없음: " + req.typeCode(), null));

        int hold = req.holdHours() != null && req.holdHours() > 0 ? req.holdHours() : 48;
        LocalDateTime until = LocalDateTime.now().plusHours(hold);

        List<Long> lockedIds = new ArrayList<>();
        int total = 0;

        // 각 (날짜, 우선순위)마다 비관적 락 걸고 AVAILABLE → LOCKED
        for (var it : req.items()) {
            var slot = bannerSlotRepository.lockAvailable(type.getId(), it.slotDate(), it.priority())
                    .orElseThrow(() -> new CustomException(
                            HttpStatus.CONFLICT,
                            "이미 선택되었거나 가용하지 않은 슬롯: " + it.slotDate() + " / priority " + it.priority(),
                            null
                    ));
            slot.setStatus(BannerSlotStatus.LOCKED);
            slot.setLockedBy(userId);
            slot.setLockedUntil(until);
            total += slot.getPrice();
            // JPA 영속 상태라 flush 시 업데이트 됨
            lockedIds.add(slot.getId());
        }
        return new LockSlotsResponseDto(lockedIds, total, until);
    }

    /** 결제 완료 → SOLD 전환 + banner 레코드 생성 + 슬롯에 sold_banner_id 연결 */
    @Transactional
    public FinalizeSoldResponseDto finalizeSold(FinalizeSoldRequestDto req, Long userId) {
        if (req == null || req.slotIds() == null || req.slotIds().isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "slotIds가 비어 있습니다.", null);
        }
        if (req.eventId() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "eventId가 필요합니다.", null);
        }
        // LOCKED → SOLD 전환
        var distinctIds = req.slotIds().stream().distinct().toList();
        int updated = bannerSlotRepository.updateStatusIfCurrentIn(
                distinctIds,
                BannerSlotStatus.SOLD,
                List.of(BannerSlotStatus.LOCKED)
        );
        if (updated != distinctIds.size()) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "SOLD 전환 실패: LOCKED 상태가 아닌 슬롯 포함 (요청 " + distinctIds.size() + "건, 성공 " + updated + "건)", null);
        }

        var slots = bannerSlotRepository.findAllWithType(distinctIds);
        slots.sort(Comparator.comparing(BannerSlot::getSlotDate).thenComparing(BannerSlot::getPriority));

        Event event = eventRepository.findById(req.eventId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "이벤트가 존재하지 않습니다: " + req.eventId(), null));
        var active = bannerStatusCodeRepository.findByCode("ACTIVE")
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "배너 상태코드(ACTIVE) 누락", null));

        List<Long> bannerIds = new ArrayList<>();

        for (BannerSlot s : slots) {
            Banner b = new Banner(
                    req.title() != null ? req.title() : "검색 상단 고정",
                    req.imageUrl(),
                    req.linkUrl(),
                    s.getPriority(),
                    s.getSlotDate().atStartOfDay(),
                    s.getSlotDate().atTime(LocalTime.of(23, 59, 59)),
                    active,                 // BannerStatusCode
                    s.getBannerType()       // BannerType
            );

            b.setEventId(req.eventId());
            b.setCreatedBy(userId);

            bannerRepository.save(b);

            bannerIds.add(b.getId());      // PK getter 일관화
            bannerSlotRepository.setSoldBanner(s.getId(), b.getId());
        }

        return new FinalizeSoldResponseDto(bannerIds);
    }

    private Banner getBannerOr404(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "존재하지 않는 배너 ID: " + id, null));
    }

    private BannerStatusCode getStatusCodeOr404(String code) {
        return bannerStatusCodeRepository.findByCode(code)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "존재하지 않는 상태 코드: " + code, null));
    }

    private BannerType getBannerTypeOr404(Long id) {
        return bannerTypeRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "존재하지 않는 배너 타입 ID: " + id, null));
    }

    private BannerResponseDto toDto(Banner banner) {
        Event event = banner.getEventId() == null ? null : eventRepository.findById(banner.getEventId()).orElse(null);
        EventDetail eventDetail = event == null ? null : event.getEventDetail();

        return BannerResponseDto.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .priority(banner.getPriority())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .statusCode(banner.getBannerStatusCode().getCode())
                .bannerTypeCode(banner.getBannerType().getCode())
                .eventId(banner.getEventId())
                .smallImageUrl(eventDetail == null ? null : eventDetail.getThumbnailUrl())
                .build();
    }


    @Transactional
    public void reorderForDate(ReorderRequestDto req) {
        if (req == null || req.type() == null || req.date() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "type/date가 비었습니다.", null);
        }

        // 정책: HERO 일괄 재정렬 금지 (필요 시 타입 화이트리스트로 전환)
           if ("HERO".equals(req.type())) {
               throw new CustomException(HttpStatus.FORBIDDEN, "HERO 배너는 일괄 재정렬을 지원하지 않습니다.", null);
               }

        LocalDate d = req.date();
        var rows = bannerRepository.lockByTypeAndDate(
                req.type(),
                d.atStartOfDay(),
                d.atTime(23, 59, 59)
        ); // 해당 날짜/타입 배너들 비관적 잠금
        if (rows.isEmpty()) return;

        // 쿼리에서 priority ASC로 가져왔으므로, 복원용 '원래 순서' 복사
        var originalOrder = new ArrayList<>(rows);

        // 충돌 방지: 전부 임시로 +1000
        rows.forEach(b -> b.setPriority(b.getPriority() + 1000));

        // 1) 요청된 배너들을 입력 순서대로 1..k
        var assigned = new java.util.HashSet<Long>();
        int p = 1;
        var items = (req.items() == null) ? java.util.List.<ReorderRequestDto.Item>of() : req.items();
          for (var it : items) {
               var b = rows.stream()
                    .filter(x -> x.getId().equals(it.bannerId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "배너 없음: " + it.bannerId(), null));
            if (assigned.add(b.getId())) {   // 중복 방지
                b.setPriority(p++);
            }
        }

        // 2) 나머지 배너들을 '원래 순서'대로 이어서 배치
        for (var b : originalOrder) {
            if (!assigned.contains(b.getId())) {
                b.setPriority(p++);
            }
        }
    }


    @Transactional(readOnly = true)
    public BannerResponseDto getOne(Long id) { return toDto(getBannerOr404(id)); }


    @Transactional(readOnly = true)
    public long countActiveBannersNow() {
        return bannerRepository.countActiveAtByType(LocalDateTime.now(), TYPE_HERO);
    }

    @Transactional(readOnly = true)
    public long countRecentBanners(int days) {
        LocalDateTime cut = LocalDateTime.now().minusDays(days);
        return bannerRepository.countRecentByType(cut, TYPE_HERO);
    }

    private static final String TYPE_HERO = "HERO";

    @Transactional(readOnly = true)
    public BigDecimal sumBannerSales() {
        Long v = bannerSlotRepository.sumSoldAmountByType(TYPE_HERO);
        return (v == null) ? BigDecimal.ZERO : BigDecimal.valueOf(v);
    }

    @Transactional(readOnly = true)
    public List<BannerResponseDto> searchVip(String type,
                                             String status,
                                             String q,
                                             LocalDateTime from,
                                             LocalDateTime to) {

        // q 전처리: 빈 문자열은 null 취급
        String qNorm = (q == null || q.isBlank()) ? null : q;

        // 기간 보정: from > to 들어오면 스왑 (선택)
        if (from != null && to != null && from.isAfter(to)) {
            LocalDateTime tmp = from;
            from = to;
            to = tmp;
        }

        return bannerRepository.search(type, status, from, to, qNorm)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HotPickDto> getActiveHotPicks(int size) {
        final int limit = Math.max(1, Math.min(size, 20));
        final LocalDateTime now = LocalDateTime.now();

        //  예매율 → priority 순으로 가져오기
        final var banners = bannerRepository.findActiveHotPicksOrderByRate(
                now, org.springframework.data.domain.PageRequest.of(0, limit)
        );

        // 2) eventId 없는 건 제외 + 개수 제한
        final List<Banner> limited = banners.stream()
                .filter(b -> b.getEventId() != null)
                .limit(limit)
                .toList();

        if (limited.isEmpty()) return List.of();

        // 3) 이벤트 로드: eventRepository PK와 일치하는 getter 사용
        //    (현재 엔티티가 getEventId()를 쓰는 구조라면 그대로 쓰세요)
        final List<Long> eventIds = limited.stream().map(Banner::getEventId).distinct().toList();

        final var eventsById = eventRepository.findAllById(eventIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.fairing.fairplay.event.entity.Event::getEventId,
                        e -> e,
                        (a, b) -> a // ← 중복 키일 때 첫 번째 값 유지 (예외 방지)
                ));

        // 4) DTO 변환 (모든 값 null-guard)
        return limited.stream().map(b -> {
            final var e = eventsById.get(b.getEventId());

            String title =
                    (e != null && e.getTitleKr() != null && !e.getTitleKr().isBlank()) ? e.getTitleKr()
                            : (e != null && e.getTitleEng() != null && !e.getTitleEng().isBlank()) ? e.getTitleEng()
                            : (b.getTitle() != null ? b.getTitle() : "");

            String date = "";
            String location = "";
            String image = (b.getImageUrl() != null && !b.getImageUrl().isBlank()) ? b.getImageUrl() : null;
            String category = "";

            if (e != null && e.getEventDetail() != null) {
                var det = e.getEventDetail();

                // 날짜
                var start = det.getStartDate();
                var end   = det.getEndDate();
                if (start != null) date = (end != null) ? (start + " ~ " + end) : start.toString();

                // 장소
                if (det.getLocationDetail() != null && !det.getLocationDetail().isBlank()) {
                    location = det.getLocationDetail();
                } else if (det.getLocation() != null) {
                    location = String.valueOf(det.getLocation()); // 리플렉션 제거
                }

                // 썸네일
                if (image == null && det.getThumbnailUrl() != null && !det.getThumbnailUrl().isBlank()) {
                    image = det.getThumbnailUrl();
                }

                // 카테고리
                if (det.getMainCategory() != null && det.getMainCategory().getGroupName() != null) {
                    category = det.getMainCategory().getGroupName();
                }
            }

            // 배너 기간 폴백
            if (date.isBlank() && b.getStartDate() != null) {
                var bs = b.getStartDate().toLocalDate();
                date = (b.getEndDate() != null) ? (bs + " ~ " + b.getEndDate().toLocalDate()) : bs.toString();
            }

            return HotPickDto.builder()
                    .id(b.getEventId())   // Long(primitive 말고)이어야 NPE 없음
                    .title(title)
                    .date(date)
                    .location(location)
                    .category(category)
                    .image(image)
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public long countExpiringAll(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusDays(days);
        return bannerRepository.countByBannerStatusCode_CodeAndEndDateBetween(
                STATUS_ACTIVE, now, until);
    }

    @Transactional(readOnly = true)
    public long countExpiringByType(int days, String typeCode) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusDays(days);
        return bannerRepository.countExpiringByType(
                STATUS_ACTIVE, typeCode, now, until);
    }


    @Transactional(readOnly = true)
    public List<BannerResponseDto> getHeroActive() {
        LocalDateTime now = LocalDateTime.now();
        return bannerRepository
                .findAllByBannerType_CodeAndBannerStatusCode_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityAsc(
                        TYPE_HERO, STATUS_ACTIVE, now, now
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BannerResponseDto> findByTypeAndStatus(String type, String status) {
        LocalDateTime now = LocalDateTime.now();
        // 널/공백 대비 & 대문자 통일
        String t = (type == null) ? "" : type.trim().toUpperCase();
        String s = (status == null) ? "" : status.trim().toUpperCase();

        return bannerRepository
                .findAllByBannerType_CodeAndBannerStatusCode_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityAsc(
                        t, s, now, now
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NewPickDto> getActiveNewPicks(int size) {
        final int limit = Math.max(1, Math.min(size, 20));
        final LocalDateTime now = LocalDateTime.now();

        // NEW + ACTIVE + 노출기간 내, priority ASC
        List<Banner> banners = bannerRepository
                .findAllByBannerType_CodeAndBannerStatusCode_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPriorityAsc(
                        "NEW", "ACTIVE", now, now
                );

        // eventId 있는 것만 제한
        List<Banner> limited = banners.stream()
                .filter(b -> b.getEventId() != null)
                .limit(limit)
                .toList();

        if (limited.isEmpty()) return List.of();

        // 이벤트 벌크 로딩
        List<Long> eventIds = limited.stream().map(Banner::getEventId).distinct().toList();
        var eventsById = eventRepository.findAllById(eventIds).stream()
                .collect(Collectors.toMap(Event::getEventId, e -> e, (a,b)->a));

        // DTO 매핑
        return limited.stream().map(b -> {
            Event e = eventsById.get(b.getEventId());

            String title =
                    (e != null && e.getTitleKr() != null && !e.getTitleKr().isBlank()) ? e.getTitleKr()
                            : (e != null && e.getTitleEng()!= null && !e.getTitleEng().isBlank()) ? e.getTitleEng()
                            : (b.getTitle() != null ? b.getTitle() : "");

            String image = (b.getImageUrl() != null && !b.getImageUrl().isBlank()) ? b.getImageUrl() : null;
            String date  = "";
            String location = "";
            String category = "";
            LocalDateTime createdAt = null;

            if (e != null && e.getEventDetail() != null) {
                var det = e.getEventDetail();

                // 날짜
                var s = det.getStartDate();
                var ed = det.getEndDate();
                if (s != null) date = (ed != null) ? (s + " ~ " + ed) : s.toString();

                // 장소
                if (det.getLocationDetail() != null && !det.getLocationDetail().isBlank()) {
                    location = det.getLocationDetail();
                } else if (det.getLocation() != null) {
                    location = String.valueOf(det.getLocation());
                }

                // 썸네일
                if (image == null && det.getThumbnailUrl() != null && !det.getThumbnailUrl().isBlank()) {
                    image = det.getThumbnailUrl();
                }

                // 카테고리
                if (det.getMainCategory() != null && det.getMainCategory().getGroupName() != null) {
                    category = det.getMainCategory().getGroupName();
                }

                // NEW 판별 기준 (가능하면 event_detail.created_at, 없으면 배너 시작일)
                createdAt = det.getCreatedAt();
            }
            if (createdAt == null) createdAt = b.getStartDate();

            return NewPickDto.builder()
                    .id(b.getEventId())
                    .title(title)
                    .image(image)
                    .date(date)
                    .location(location)
                    .category(category)
                    .createdAt(createdAt)
                    .build();
        }).toList();
    }

}