// BannerSlotService.java
package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.SlotResponseDto;
import com.fairing.fairplay.banner.entity.BannerSlotStatus;
import com.fairing.fairplay.banner.repository.BannerSlotRepository;
import com.fairing.fairplay.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BannerSlotService {

    private final JdbcTemplate jdbc;
    private final BannerSlotRepository bannerSlotRepository;

    @Transactional(readOnly = true) //  읽기 전용
    public List<SlotResponseDto> getSlots(String typeCode, LocalDate from, LocalDate to) {
        String sql = """
            SELECT s.slot_date, s.priority, s.status, s.price
            FROM banner_slot s
            JOIN banner_type bt ON bt.banner_type_id = s.banner_type_id
            WHERE bt.code = ?
              AND s.slot_date BETWEEN ? AND ?
            ORDER BY s.slot_date, s.priority
        """;
        return jdbc.query(sql, (ResultSet rs, int i) -> new SlotResponseDto(
                rs.getDate("slot_date") != null ? rs.getDate("slot_date").toLocalDate() : null,
                rs.getInt("priority"),
                BannerSlotStatus.valueOf(rs.getString("status")),
                rs.getInt("price")
        ), typeCode, from, to);
    }

    @Transactional //  쓰기 트랜잭션
    public void markSold(List<Long> slotIds, Long bannerId) {
        if (slotIds == null || slotIds.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "slotIds가 비어 있습니다.", null);
        }

        int updated = bannerSlotRepository.markSold(
                slotIds,
                bannerId,
                BannerSlotStatus.SOLD,
                List.of(BannerSlotStatus.LOCKED) // 필요시 상태 더 추가
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
