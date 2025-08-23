package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.SlotResponseDto;
import com.fairing.fairplay.banner.entity.BannerSlot;
import com.fairing.fairplay.banner.entity.BannerSlotStatus;
import com.fairing.fairplay.banner.entity.BannerSlotType;
import com.fairing.fairplay.banner.repository.BannerSlotRepository;
import com.fairing.fairplay.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BannerSlotService {

    private final BannerSlotRepository bannerSlotRepository;

    @Transactional(readOnly = true) //  읽기 전용으로 되돌림
    public List<SlotResponseDto> getSlots(BannerSlotType type, LocalDate from, LocalDate to) {
        if (type == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "배너 타입이 비었습니다.", null);
        }
        if (from == null || to == null || from.isAfter(to)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "날짜 범위가 올바르지 않습니다.", null);
        }

        // 슬롯 데이터 조회
        List<BannerSlot> slots = bannerSlotRepository
                .findByBannerType_CodeAndSlotDateBetweenOrderBySlotDateAscPriorityAsc(type.name(), from, to);
        
        // banner_application_slot에 이미 있는 슬롯들 조회 (Native Query로 안전하게)
        Set<Long> reservedSlotIds = getReservedSlotIds();
        
        return slots.stream()
                .map(s -> {
                    // 이미 신청된 슬롯은 SOLD로 표시
                    BannerSlotStatus status = reservedSlotIds.contains(s.getId()) ? 
                        BannerSlotStatus.SOLD : s.getStatus();
                    return new SlotResponseDto(s.getSlotDate(), s.getPriority(), status, s.getPrice());
                })
                .toList();
    }

    /**
     * 만료된 LOCK 상태를 AVAILABLE로 변경
     */
    public void cleanupExpiredLocks() {
        try {
            bannerSlotRepository.updateExpiredLocksToAvailable();
        } catch (Exception e) {
            // 만료된 락 정리 실패는 무시하고 계속 진행
            System.err.println("만료된 락 정리 실패: " + e.getMessage());
        }
    }

    /**
     * banner_application_slot에 이미 있는 슬롯 ID들을 조회
     */
    private Set<Long> getReservedSlotIds() {
        try {
            return new HashSet<>(bannerSlotRepository.findReservedSlotIds());
        } catch (Exception e) {
            // 조회 실패 시 빈 Set 반환 (기본 상태 유지)
            return new HashSet<>();
        }
    }


    @Transactional //  쓰기 트랜잭션
    public void markSold(List<Long> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "slotIds가 비어 있습니다.", null);
        }

        var distinctIds = slotIds.stream().distinct().toList();
        int updated = bannerSlotRepository.updateStatusIfCurrentIn(
                distinctIds,
                BannerSlotStatus.SOLD,
                List.of(BannerSlotStatus.LOCKED)
        );


        if (updated != distinctIds.size()) {
            throw new CustomException(
                    HttpStatus.CONFLICT,
                    "SOLD 전환 실패: LOCKED 상태가 아닌 슬롯 포함 (요청 " + distinctIds.size() + "건, 성공 " + updated + "건)",
                    null
            );
        }
    }
}