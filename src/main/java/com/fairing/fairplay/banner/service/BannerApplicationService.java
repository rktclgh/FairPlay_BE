package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.CreateApplicationRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BannerApplicationService {

    // 상태 코드 문자열 상수
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String SLOT_STATUS_AVAILABLE = "AVAILABLE";
    private static final String SLOT_STATUS_LOCKED = "LOCKED";
    private static final String SLOT_STATUS_SOLD = "SOLD";
    private static final String BANNER_STATUS_ACTIVE = "ACTIVE";

    // 기본 락 시간(분) — 48시간
    private static final int DEFAULT_LOCK_MINUTES = 48 * 60;

    // 하루 종료 시간 상수
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    @Value("${banner.lock.default-minutes:" + DEFAULT_LOCK_MINUTES + "}")
    private int configuredLockMinutes;

    private Long typeId(String code) {
        try {
            return jdbc.queryForObject(
                    "SELECT banner_type_id FROM banner_type WHERE code = ?",
                    Long.class, code
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("존재하지 않는 배너 타입: " + code);
        }
    }

    private Integer statusId(String code) {
        try {
            return jdbc.queryForObject(
                    "SELECT apply_status_code_id FROM apply_status_code WHERE code = ?",
                    Integer.class, code
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("존재하지 않는 신청 상태 코드: " + code);
        }
    }

    private Integer bannerStatusId(String code) {
        try {
            return jdbc.queryForObject(
                    "SELECT banner_status_code_id FROM banner_status_code WHERE code = ?",
                    Integer.class, code
            );
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("존재하지 않는 배너 상태 코드: " + code);
        }
    }

    /** 신청 + 슬롯 LOCK (원자적) */
    @Transactional
    public Long createApplicationAndLock(CreateApplicationRequestDto req, Long userId) {
        long typeId = typeId(req.bannerType().name());
        int lockMinutes = Optional.ofNullable(req.lockMinutes()).orElse(configuredLockMinutes);

        // 1) 대상 슬롯 잠그기 위해 slot_id, price 조회 (FOR UPDATE)
        List<Long> slotIds = new ArrayList<>();
        List<Integer> prices = new ArrayList<>();
        for (CreateApplicationRequestDto.Item it : req.items()) {
            Map<String, Object> result;
            try {
                result = jdbc.queryForMap("""
                        SELECT slot_id, price
                        FROM banner_slot
                        WHERE banner_type_id = ?
                          AND slot_date = ?
                          AND priority = ?
                          AND status = ?
                        ORDER BY slot_id
                        FOR UPDATE
                        """, typeId, it.date(), it.priority(), SLOT_STATUS_AVAILABLE);
            } catch (EmptyResultDataAccessException e) {
                throw new IllegalStateException("매진/선점된 슬롯 있음: " + it);
            }
            slotIds.add((Long) result.get("slot_id"));
            prices.add((Integer) result.get("price"));
        }

        // 2) LOCK 전환
        Map<String, Object> params = Map.of(
                "userId", userId,
                "lockMinutes", lockMinutes,
                "slotIds", slotIds
        );

        int locked = namedJdbc.update("""
                UPDATE banner_slot
                   SET status=:locked,
                       locked_by=:userId,
                       locked_until = DATE_ADD(NOW(), INTERVAL :lockMinutes MINUTE)
                 WHERE slot_id IN (:slotIds)
                   AND status=:available
                """, new MapSqlParameterSource(params)
                .addValue("locked", SLOT_STATUS_LOCKED)
                .addValue("available", SLOT_STATUS_AVAILABLE));

        if (locked != slotIds.size()) {
            throw new IllegalStateException(
                    String.format("LOCK 실패: 이미 점유된 슬롯 포함 (요청 %d, 성공 %d)", slotIds.size(), locked)
            );
        }

        // 3) 신청서 저장
        int total = prices.stream().mapToInt(Integer::intValue).sum();
        Integer pendingId = statusId(STATUS_PENDING);

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
            var first = req.items().get(0);
            ps.setLong(1, req.eventId());
            ps.setLong(2, userId);
            ps.setLong(3, typeId);
            ps.setString(4, req.title());
            ps.setString(5, req.imageUrl());
            ps.setString(6, req.linkUrl());
            ps.setInt(7, first.priority());
            ps.setTimestamp(8, Timestamp.valueOf(first.date().atStartOfDay()));
            ps.setTimestamp(9, Timestamp.valueOf(lastDate(req.items()).atTime(END_OF_DAY)));
            ps.setInt(10, pendingId);
            ps.setInt(11, total);
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, kh);
        long appId = Objects.requireNonNull(kh.getKey()).longValue();

        // 4) 신청-슬롯 매핑 + 가격 스냅샷
        String sql = "INSERT INTO banner_application_slot (banner_application_id, slot_id, item_price) VALUES (?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<>();
        for (int i = 0; i < slotIds.size(); i++) {
            batchArgs.add(new Object[]{appId, slotIds.get(i), prices.get(i)});
        }
        jdbc.batchUpdate(sql, batchArgs);

        return appId;
    }

    private LocalDate lastDate(List<CreateApplicationRequestDto.Item> items) {
        return items.stream()
                .map(CreateApplicationRequestDto.Item::date)
                .max(LocalDate::compareTo)
                .orElseThrow();
    }

    /** 신청 취소(본인 락 해제) */
    @Transactional
    public void cancelApplication(Long appId, Long userId) {
        // 1) 본인 신청인지 검증
        Long owner;
        try {
            owner = jdbc.queryForObject("""
                    SELECT applicant_id FROM banner_application WHERE banner_application_id=?
                    """, Long.class, appId);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("존재하지 않는 신청입니다: " + appId);
        }
        if (!Objects.equals(owner, userId)) {
            throw new AccessDeniedException("해당 신청을 취소할 권한이 없습니다");
        }

        // 2) 매핑된 슬롯 조회
        List<Long> slotIds = jdbc.queryForList("""
                SELECT slot_id FROM banner_application_slot WHERE banner_application_id=?
                """, Long.class, appId);

        // 3) 락 해제
        if (!slotIds.isEmpty()) {
            var params = new MapSqlParameterSource()
                    .addValue("slotIds", slotIds)
                    .addValue("userId", userId)
                    .addValue("locked", SLOT_STATUS_LOCKED)
                    .addValue("available", SLOT_STATUS_AVAILABLE);

            int unlocked = namedJdbc.update("""
                    UPDATE banner_slot
                       SET status=:available, locked_by=NULL, locked_until=NULL
                     WHERE slot_id IN (:slotIds)
                       AND locked_by = :userId
                       AND status = :locked
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
        // markPaid 시작부에 추가
        Integer pendingId = statusId(STATUS_PENDING);
        Integer appStatus = jdbc.queryForObject(
                "SELECT status_code_id FROM banner_application WHERE banner_application_id=? FOR UPDATE",
                Integer.class, appId
        );
        if (!Objects.equals(appStatus, pendingId)) {
            throw new IllegalStateException("승인할 수 없는 상태입니다.");
        }

        // 슬롯 잠그기
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
        if (slots.stream().anyMatch(m -> !SLOT_STATUS_LOCKED.equals(m.get("status"))))
            throw new IllegalStateException("LOCKED 아님");

        // 신청서 정보
        Map<String, Object> app;
        try {
            app = jdbc.queryForMap("""
                    SELECT event_id, title, image_url, link_url, banner_type_id
                    FROM banner_application WHERE banner_application_id=?
                    """, appId);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("존재하지 않는 신청입니다: " + appId);
        }

        // 1) SOLD 전환
        var slotIdList = slots.stream()
                .map(m -> (Long) m.get("slotId"))
                .toList();

        var soldParams = new MapSqlParameterSource()
                .addValue("slotIds", slotIdList)
                .addValue("locked", SLOT_STATUS_LOCKED);

        int sold = namedJdbc.update("""
                UPDATE banner_slot
                   SET status=:sold
                 WHERE slot_id IN (:slotIds)
                   AND status=:locked
                """, soldParams.addValue("sold", SLOT_STATUS_SOLD));

        if (sold != slots.size()) {
            throw new IllegalStateException(
                    String.format("SOLD 전환 실패: LOCKED 아닌 슬롯 존재 (요청 %d, 성공 %d)", slots.size(), sold)
            );
        }

        // 2) 배너 생성
        Integer activeId = bannerStatusId(BANNER_STATUS_ACTIVE);
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

                ps.setString(1, (String) app.get("title"));
                ps.setString(2, (String) app.get("image_url"));
                ps.setString(3, (String) app.get("link_url"));
                ps.setLong(4, ((Number) app.get("event_id")).longValue());
                ps.setLong(5, adminId);
                ps.setInt(6, (Integer) m.get("priority"));
                ps.setTimestamp(7, Timestamp.valueOf(d.atStartOfDay()));
                ps.setTimestamp(8, Timestamp.valueOf(d.atTime(END_OF_DAY)));
                ps.setInt(9, activeId);
                ps.setLong(10, typeId);
                return ps;
            }, kh);

            Long bannerId = Objects.requireNonNull(kh.getKey()).longValue();

            // 슬롯에 배너 매핑
            var mapParams = new MapSqlParameterSource()
                    .addValue("bannerId", bannerId)
                    .addValue("slotId", (Long) m.get("slotId"));

            namedJdbc.update("""
                    UPDATE banner_slot
                       SET sold_banner_id = :bannerId,
                           locked_by = NULL,
                           locked_until = NULL
                     WHERE slot_id = :slotId
                       AND status = :sold
                    """, mapParams.addValue("sold", SLOT_STATUS_SOLD));
        }

        // 3) 신청 상태 승인 처리
        int updated = jdbc.update("""
                  UPDATE banner_application
                     SET status_code_id = (SELECT apply_status_code_id FROM apply_status_code WHERE code='APPROVED'),
                         approved_by=?, approved_at=NOW()
                   WHERE banner_application_id=? 
                     AND status_code_id = (SELECT apply_status_code_id FROM apply_status_code WHERE code='PENDING')
                """, adminId, appId);
        if (updated != 1) throw new IllegalStateException("신청 상태가 변경되어 승인 처리에 실패했습니다.");
}

        // com.fairing.fairplay.banner.service.BannerApplicationService
        @Transactional
        public void reject (Long appId, Long adminId, String reason){
            // 현재 신청 상태 확인 (PENDING 인지 등)
            Integer pendingId = statusId(STATUS_PENDING);
            Integer currentStatus;
            try {
                currentStatus = jdbc.queryForObject(
                        "SELECT status_code_id FROM banner_application WHERE banner_application_id=? FOR UPDATE",
                        Integer.class, appId
                );
            } catch (EmptyResultDataAccessException e) {
                throw new IllegalArgumentException("존재하지 않는 신청입니다: " + appId);
            }
            if (!Objects.equals(currentStatus, pendingId)) {
                throw new IllegalStateException("반려할 수 없는 상태입니다.");
            }

            // 잠금 슬롯 되돌리기 (LOCKED → AVAILABLE, 본인 소유자 여부는 묻지 않음: 관리자 권한)
            List<Long> slotIds = jdbc.queryForList("""
                        SELECT slot_id FROM banner_application_slot WHERE banner_application_id=?
                    """, Long.class, appId);

            if (!slotIds.isEmpty()) {
                namedJdbc.update("""
                            UPDATE banner_slot
                               SET status='AVAILABLE', locked_by=NULL, locked_until=NULL
                             WHERE slot_id IN (:slotIds) AND status='LOCKED'
                        """, new MapSqlParameterSource().addValue("slotIds", slotIds));
            }

            // 신청 상태 REJECTED + 사유 기록
            int updated = jdbc.update("""
                      UPDATE banner_application
                         SET status_code_id = (SELECT apply_status_code_id FROM apply_status_code WHERE code='REJECTED'),
                             admin_comment = ?, approved_by=?, approved_at=NOW()
                       WHERE banner_application_id=? 
                         AND status_code_id = (SELECT apply_status_code_id FROM apply_status_code WHERE code='PENDING')
                    """, reason, adminId, appId);
            if (updated != 1) throw new IllegalStateException("신청 상태가 변경되어 반려 처리에 실패했습니다.");


        }
    }