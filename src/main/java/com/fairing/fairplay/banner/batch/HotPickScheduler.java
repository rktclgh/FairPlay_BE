package com.fairing.fairplay.banner.batch;

import com.fairing.fairplay.banner.entity.Banner;
import com.fairing.fairplay.banner.entity.BannerStatusCode;
import com.fairing.fairplay.banner.entity.BannerType;
import com.fairing.fairplay.banner.repository.BannerRepository;
import com.fairing.fairplay.banner.repository.BannerStatusCodeRepository;
import com.fairing.fairplay.banner.repository.BannerTypeRepository;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotPickScheduler {

    private final BannerRepository bannerRepository;
    private final BannerStatusCodeRepository statusRepo;
    private final BannerTypeRepository typeRepo;
    private final ReservationRepository reservationRepository;

    private static final int HOT_PICK_LIMIT = 5; // 상위 N개

    @Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시
    @Transactional
    public void updateHotPicks() {
        BannerType hotPickType = typeRepo.findByCode("HOT_PICK")
                .orElseThrow(() -> new IllegalStateException("HOT_PICK 배너 타입 없음"));
        BannerStatusCode active = statusRepo.findByCode("ACTIVE")
                .orElseThrow(() -> new IllegalStateException("ACTIVE 상태 코드 없음"));
        BannerStatusCode inactive = statusRepo.findByCode("INACTIVE")
                .orElseThrow(() -> new IllegalStateException("INACTIVE 상태 코드 없음"));

        // 기존 HOT_PICK 모두 비활성화
        bannerRepository.deactivateAllActiveByType(hotPickType.getCode(), "ACTIVE", inactive);


// 예매 수량 상위 이벤트 조회 (CONFIRMED 만 포함)
        List<Object[]> ranking = reservationRepository
                .findEventBookingQuantities(java.util.List.of("CONFIRMED"));

        // 예매율 순위 조회
        List<Object[]> bookingRates = reservationRepository.findEventBookingRates("CONFIRMED");

        int prio = 0;
        int created = 0;

        LocalDateTime startAt = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime endAt   = startAt.plusDays(7);

        for (Object[] row : bookingRates) {
            if (created >= HOT_PICK_LIMIT) break;
            Long eventId = ((Number) row[0]).longValue();


            // 같은 이벤트가 이미 HOT_PICK ACTIVE 상태면 건너뜀 (선택)
            if (bannerRepository.existsByBannerType_CodeAndEventIdAndBannerStatusCode_Code(
                    hotPickType.getCode(), eventId, active.getCode())) {
                continue;
            }


                    Banner banner = new Banner(
                            "Hot Pick" + eventId,
                            "",  // image_url (TODO: 이벤트/파일에서 가져오기)
                            null,
                            prio++,             // <- 0,1,2… UK 충돌 방지
                            startAt,
                            endAt,
                            active,
                            hotPickType
                    );
                    banner.setEventId(eventId);
                    bannerRepository.save(banner);
            created++;

                }


        log.info("HOT_PICK 배너 {}건 생성 완료", created);


 }
}

