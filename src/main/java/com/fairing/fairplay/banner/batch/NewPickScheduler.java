package com.fairing.fairplay.banner.batch;

import com.fairing.fairplay.banner.entity.Banner;
import com.fairing.fairplay.banner.entity.BannerStatusCode;
import com.fairing.fairplay.banner.entity.BannerType;
import com.fairing.fairplay.banner.repository.BannerRepository;
import com.fairing.fairplay.banner.repository.BannerStatusCodeRepository;
import com.fairing.fairplay.banner.repository.BannerTypeRepository;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
//@Profile("local")

public class NewPickScheduler {

    private final BannerRepository bannerRepository;
    private final BannerStatusCodeRepository statusRepo;
    private final BannerTypeRepository typeRepo;
    private final EventRepository eventRepository;

    @Value("${banner.new.limit:5}")
    private int newLimit;

    @Value("${banner.new.expose-days:7}")
    private int exposeDays;

    //@Scheduled(cron = "0 5 2 * * *")
    //@Scheduled(cron = "0 */1 * * * *") // 임시: 1분마다

    @Transactional
    public void updateNewPicks() {
        BannerType newType = typeRepo.findByCode("NEW")
                .orElseThrow(() -> new IllegalStateException("NEW 배너 타입 없음"));
        BannerStatusCode active = statusRepo.findByCode("ACTIVE")
                .orElseThrow(() -> new IllegalStateException("ACTIVE 상태 코드 없음"));
        BannerStatusCode inactive = statusRepo.findByCode("INACTIVE")
                .orElseThrow(() -> new IllegalStateException("INACTIVE 상태 코드 없음"));

        // 기존 NEW 중 ACTIVE만 비활성화 (히스토리 보존)
        bannerRepository.deactivateAllActiveByType(newType.getCode(), "ACTIVE", inactive);

        // 다른 타입과 중복 노출 제외 목록
        List<Long> excludeIds = bannerRepository.findActiveEventIdsInTypes(
                java.util.Arrays.asList("HERO","SEARCH_TOP","MD_PICK","HOT_PICK")
        );

        // 최신 이벤트 N개 (event_detail.created_at 기준) — 리포지토리 시그니처는 List 반환 그대로 사용
        List<Event> latest =
                eventRepository.findByEventDetailIsNotNullOrderByEventDetail_CreatedAtDesc(
                        PageRequest.of(0, newLimit)
                );

        int prio = 0;
        LocalDateTime startAt = LocalDateTime.now();
        LocalDateTime endAt   = startAt.plusDays(exposeDays);
        int created = 0;
        for (Event ev : latest) {
            if (created >= newLimit) break;

            Long eventId = ev.getEventId();
            if (excludeIds.contains(eventId)) continue; // 타 타입과 중복 방지

            // 이미 NEW 타입으로 ACTIVE면 스킵
            if (bannerRepository.existsByBannerType_CodeAndEventIdAndBannerStatusCode_Code(
                    newType.getCode(), eventId, active.getCode())) {
                continue;
            }

            String title = ev.getTitleKr();
            if (title == null || title.isBlank()) title = ev.getTitleEng();

            String thumb = null;
            if (ev.getEventDetail() != null) {
                thumb = ev.getEventDetail().getThumbnailUrl();
            }

            Banner b = new Banner(
                    (title != null ? title : ("New Event " + eventId)),
                    thumb,                 // null이면 프론트/엔티티에서 기본 이미지 처리
                    null,
                    prio++,
                    startAt,          // ← 동일 시작
                    endAt,
                    active,
                    newType
            );
            b.setEventId(eventId);
            bannerRepository.save(b);
            created++;
        }

        log.info("NEW 배너 {}건 생성 완료", created); //  실제 생성 건수 기준 로그
    }
}
