// BannerSlotService.java
package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.SlotResponseDto;
import com.fairing.fairplay.banner.dto.SlotResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerSlotService {

    private final JdbcTemplate jdbc;

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
                rs.getDate("slot_date").toLocalDate(),
                rs.getInt("priority"),
                rs.getString("status"),
                rs.getInt("price")
        ), typeCode, from, to);
    }
}
