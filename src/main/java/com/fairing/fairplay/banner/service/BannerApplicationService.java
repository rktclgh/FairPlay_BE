package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.CreateApplicationRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.stream.Collectors.joining;


@Service
@RequiredArgsConstructor
public class BannerApplicationService {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private Long typeId(String code) {
        Long id = jdbc.queryForObject(
                           "SELECT banner_type_id FROM banner_type WHERE code = ?",
                           Long.class, code);
           if (id == null) {
                   throw new IllegalArgumentException("존재하지 않는 배너 타입: " + code);
           }
           return id;
    }

    private Integer statusId(String code) {
        return jdbc.queryForObject(
                "SELECT apply_status_code_id FROM apply_status_code WHERE code = ?",
                Integer.class, code);
    }

    private Integer bannerStatusId(String code) {
        return jdbc.queryForObject(
                "SELECT banner_status_code_id FROM banner_status_code WHERE code = ?",
                Integer.class, code);
    }

    /** 신청 + 슬롯 LOCK (원자적) */
    @Transactional
    public Long createApplicationAndLock(CreateApplicationRequestDto req, Long userId) {
        Long typeId = typeId(req.bannerTypeCode());
        int lockMinutes = Optional.ofNullable(req.lockMinutes()).orElse(2880); // 48h

        // 1) 대상 슬롯들 잠그기 위해 slot_id, price 조회 (FOR UPDATE)
        List<Long> slotIds = new ArrayList<>();
        List<Integer> prices = new ArrayList<>();
        for (CreateApplicationRequestDto.Item it : req.items()) {
            Long slotId = jdbc.query("""
                SELECT slot_id
                FROM banner_slot
                WHERE banner_type_id = ?
                  AND slot_date = ?
                  AND priority = ?
                  AND status = 'AVAILABLE'
                  ORDER BY slot_id
                FOR UPDATE
            """, rs -> rs.next() ? rs.getLong(1) : null,
                    typeId, it.date(), it.priority());

            if (slotId == null) throw new IllegalStateException("매진/선점된 슬롯 있음: " + it);

            Integer price = jdbc.queryForObject("""
                SELECT price FROM banner_slot WHERE slot_id = ?
            """, Integer.class, slotId);

            slotIds.add(slotId);
            prices.add(price);
        }

        // 2) LOCK 전환
        Map<String, Object> params = new HashMap<>();
                params.put("userId", userId);
                params.put("lockMinutes", lockMinutes);
                params.put("slotIds", slotIds);

        int locked = namedJdbc.update("""
    UPDATE banner_slot
       SET status='LOCKED',
           locked_by=:userId,
           locked_until = DATE_ADD(NOW(), INTERVAL :lockMinutes MINUTE)
     WHERE slot_id IN (:slotIds)
       AND status='AVAILABLE'
""", params);

        if (locked != slotIds.size()) {
            throw new IllegalStateException("LOCK 실패: 이미 점유된 슬롯 포함 (요청 %d, 성공 %d)"
                    .formatted(slotIds.size(), locked));
        }

        // 3) 신청서 저장
        int total = prices.stream().mapToInt(Integer::intValue).sum();
        Integer pendingId = statusId("PENDING");

        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("""
                INSERT INTO banner_application (
                  event_id, applicant_id, banner_type_id,
                  title, image_url, link_url,
                  requested_priority, start_date, end_date,
                  status_code_id, total_amount, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, PreparedStatement.RETURN_GENERATED_KEYS);
            // requested_priority/start_date/end_date는 일단 첫 아이템 기준(운영 편의용 표시 필드)
            var first = req.items().get(0);
            ps.setLong(1, req.eventId());
            ps.setLong(2, userId);
            ps.setLong(3, typeId);
            ps.setString(4, req.title());
            ps.setString(5, req.imageUrl());
            ps.setString(6, req.linkUrl());
            ps.setInt(7, first.priority());
            ps.setTimestamp(8, Timestamp.valueOf(first.date().atStartOfDay()));
            ps.setTimestamp(9, Timestamp.valueOf(lastDate(req.items()).atTime(23,59,59)));
            ps.setInt(10, pendingId);
            ps.setInt(11, total);
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, kh);
        long appId = Objects.requireNonNull(kh.getKey()).longValue();

        // 4) 신청-슬롯 매핑 + 가격 스냅샷
        String values = slotIds.stream().map(id -> "(?, ?, ?)").collect(joining(","));
        jdbc.update(con -> {
            var ps = con.prepareStatement("""
                INSERT INTO banner_application_slot (banner_application_id, slot_id, item_price)
                VALUES %s
            """.formatted(values));
            int idx = 1;
            for (int i=0;i<slotIds.size();i++) {
                ps.setLong(idx++, appId);
                ps.setLong(idx++, slotIds.get(i));
                ps.setInt(idx++, prices.get(i));
            }
            return ps;
        });

        return appId;
    }

    private LocalDate lastDate(List<CreateApplicationRequestDto.Item> items) {
        return items.stream().map(CreateApplicationRequestDto.Item::date).max(LocalDate::compareTo).orElseThrow();
    }

    /** 신청 취소(본인 락 해제) */
    @Transactional
    public void cancelApplication(Long appId, Long userId) {
        // 1) 본인 신청인지 검증
        Long owner = jdbc.queryForObject("""
            SELECT applicant_id FROM banner_application WHERE banner_application_id=?
        """, Long.class, appId);
        if (!Objects.equals(owner, userId)) {
                        throw new AccessDeniedException("해당 신청을 취소할 권한이 없습니다");
                    }
        // 2) 매핑된 슬롯 조회
        List<Long> slotIds = jdbc.queryForList("""
          SELECT slot_id FROM banner_application_slot WHERE banner_application_id=?
        """, Long.class, appId);

        // 3) 취소 시 락이 풀렸는지 검증
        if (!slotIds.isEmpty()) {
            var params = new HashMap<String, Object>();
            params.put("slotIds", slotIds);
            params.put("userId", userId);

            int unlocked = namedJdbc.update("""
            UPDATE banner_slot
               SET status='AVAILABLE', locked_by=NULL, locked_until=NULL
             WHERE slot_id IN (:slotIds)
               AND locked_by = :userId
               AND status = 'LOCKED'
        """, params);

            if (unlocked != slotIds.size()) {
                throw new IllegalStateException(
                        String.format("취소 실패: 일부 슬롯이 LOCKED 상태/소유자가 아님 (요청 %d, 성공 %d)", slotIds.size(), unlocked)
                );
            }
        }


        // 4) 매핑/신청 데이터 삭제
        jdbc.update("DELETE FROM banner_application_slot WHERE banner_application_id=?", appId);
        jdbc.update("DELETE FROM banner_application WHERE banner_application_id=?", appId);
    }

    /** 결제 성공 처리 → SOLD + 배너 생성 */
    @Transactional
    public void markPaid(Long appId, Long adminId) {
        // 잠그기
        var slots = jdbc.query("""
            SELECT s.slot_id, s.banner_type_id, s.slot_date, s.priority, s.status
            FROM banner_application_slot asx
            JOIN banner_slot s ON s.slot_id = asx.slot_id
            WHERE asx.banner_application_id = ?
            FOR UPDATE
        """, (rs, i) -> Map.of(
                "slotId", rs.getLong("slot_id"),
                "typeId", rs.getLong("banner_type_id"),
                "slotDate", rs.getDate("slot_date").toLocalDate(),
                "priority", rs.getInt("priority"),
                "status", rs.getString("status")
        ), appId);

        if (slots.isEmpty()) throw new IllegalStateException("신청 슬롯 없음");
        if (slots.stream().anyMatch(m -> !"LOCKED".equals(m.get("status"))))
            throw new IllegalStateException("LOCKED 아님");

        // 신청서 정보
        var app = jdbc.queryForMap("""
            SELECT event_id, title, image_url, link_url, banner_type_id
            FROM banner_application WHERE banner_application_id=?
        """, appId);

        // 1) SOLD 전환 (LOCKED인 것만 허용) + 건수 검증
        var slotIdList = slots.stream()
                .map(m -> (Long) m.get("slotId"))
                .collect(java.util.stream.Collectors.toList());

        var soldParams = new MapSqlParameterSource().addValue("slotIds", slotIdList);

        int sold = namedJdbc.update("""
        UPDATE banner_slot
           SET status='SOLD'
         WHERE slot_id IN (:slotIds)
           AND status='LOCKED'
    """, soldParams);

        if (sold != slots.size()) {
            throw new IllegalStateException(
                    String.format("SOLD 전환 실패: LOCKED 아닌 슬롯 존재 (요청 %d, 성공 %d)", slots.size(), sold)
            );
        }

        // 2) 배너 생성(하루 1건씩) → 슬롯 매핑 + 락 정리는 루프 안에서
        Integer activeId = bannerStatusId("ACTIVE");
        Long typeId = ((Number) app.get("banner_type_id")).longValue();

        for (var m : slots) {
            LocalDate d = (LocalDate) m.get("slotDate");

            KeyHolder kh = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement("""
                INSERT INTO banner (
                  title, image_url, link_url,
                  event_id, created_by, priority,
                  start_date, end_date,
                  banner_status_code_id, banner_type_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, Statement.RETURN_GENERATED_KEYS);

                ps.setString(1,  (String)  app.get("title"));
                ps.setString(2,  (String)  app.get("image_url"));
                ps.setString(3,  (String)  app.get("link_url"));
                ps.setLong(4,   ((Number) app.get("event_id")).longValue());
                ps.setLong(5,   adminId);
                ps.setInt(6,    (Integer) m.get("priority"));
                ps.setTimestamp(7, Timestamp.valueOf(d.atStartOfDay()));
                ps.setTimestamp(8, Timestamp.valueOf(d.atTime(23, 59, 59)));
                ps.setInt(9,    activeId);
                ps.setLong(10,  typeId);
                return ps;
            }, kh);

            Long bannerId = java.util.Objects.requireNonNull(kh.getKey()).longValue();

            // 배너 생성 직후, 해당 슬롯에 배너ID 매핑 + 락 필드 정리
            var mapParams = new MapSqlParameterSource()
                    .addValue("bannerId", bannerId)
                    .addValue("slotId", (Long) m.get("slotId"));

            namedJdbc.update("""
            UPDATE banner_slot
               SET sold_banner_id = :bannerId,
                   locked_by = NULL,
                   locked_until = NULL
             WHERE slot_id = :slotId
               AND status = 'SOLD'
        """, mapParams);
        }

        // 신청 상태 업데이트(승인)
        jdbc.update("""
            UPDATE banner_application
            SET status_code_id = (SELECT apply_status_code_id FROM apply_status_code WHERE code='APPROVED'),
                approved_by=?, approved_at=NOW()
            WHERE banner_application_id=?
        """, adminId, appId);
    }
}
