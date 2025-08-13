package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.SlotResponseDto;
import com.fairing.fairplay.banner.entity.BannerSlotStatus;
import com.fairing.fairplay.banner.entity.BannerSlotType;
import com.fairing.fairplay.banner.repository.BannerSlotRepository;
import com.fairing.fairplay.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BannerSlotService {

    private final BannerSlotRepository bannerSlotRepository;

    @Transactional(readOnly = true) //  읽기 전용
    public List<SlotResponseDto> getSlots(BannerSlotType type, LocalDate from, LocalDate to) {
        if (type == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "배너 타입이 비었습니다.", null);
        }
        if (from == null || to == null || from.isAfter(to)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "날짜 범위가 올바르지 않습니다.", null);
        }

        // 기존 리포지토리 메서드가 bannerType.code(String)을 받으므로 name()으로 전달
        return bannerSlotRepository
                .findByBannerType_CodeAndSlotDateBetweenOrderBySlotDateAscPriorityAsc(type.name(), from, to)
                .stream()
                .map(s -> new SlotResponseDto(s.getSlotDate(), s.getPriority(), s.getStatus(), s.getPrice()))
                .toList();
    }


    @Transactional //  쓰기 트랜잭션
    public void markSold(List<Long> slotIds) {
        if (slotIds == null || slotIds.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "slotIds가 비어 있습니다.", null);
        }

        int updated = bannerSlotRepository.updateStatusIfCurrentIn(
                slotIds,
                BannerSlotStatus.SOLD,
                List.of(BannerSlotStatus.LOCKED)
        );


        if (updated != slotIds.size()) {
            throw new CustomException(
                    HttpStatus.CONFLICT,
                    "SOLD 전환 실패: LOCKED 상태가 아닌 슬롯 포함 (요청 " + slotIds.size() + "건, 성공 " + updated + "건)",
                    null
            );
        }
    }
}