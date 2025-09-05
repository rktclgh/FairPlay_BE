package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.AdminApplicationListItemDto;
import com.fairing.fairplay.banner.dto.AdminApplicationSlotDto;
import com.fairing.fairplay.banner.dto.CreateApplicationRequestDto;
import com.fairing.fairplay.banner.entity.BannerApplication;
import com.fairing.fairplay.banner.entity.BannerApplicationSlot;
import com.fairing.fairplay.banner.repository.BannerApplicationRepository;
import com.fairing.fairplay.banner.repository.BannerApplicationSlotRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.email.service.BannerEmailService;
import com.fairing.fairplay.core.service.LocalFileService;
// import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.event.entity.ApplyStatusCode;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.ApplyStatusCodeRepository;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.PaymentStatusCode;
import com.fairing.fairplay.payment.entity.PaymentTargetType;
import com.fairing.fairplay.payment.entity.PaymentTypeCode;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.payment.repository.PaymentStatusCodeRepository;
import com.fairing.fairplay.payment.repository.PaymentTargetTypeRepository;
import com.fairing.fairplay.payment.repository.PaymentTypeCodeRepository;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final BannerEmailService bannerEmailService;
    private final UserRepository userRepository;
    private final BannerApplicationRepository bannerApplicationRepository;
    private final BannerApplicationSlotRepository bannerApplicationSlotRepository;
    private final EventRepository eventRepository;
    // private final AwsS3Service awsS3Service;
    private final LocalFileService localFileService;
    private final PaymentRepository paymentRepository;
    private final PaymentStatusCodeRepository paymentStatusCodeRepository;
    private final PaymentTargetTypeRepository paymentTargetTypeRepository;
    private final PaymentTypeCodeRepository paymentTypeCodeRepository;
    private final ApplyStatusCodeRepository applyStatusCodeRepository;

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

        List<Event> events = eventRepository.findByManager_User_UserId(userId);
        Event event = events.getFirst();

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

        // 이미지 URL 처리: Local 파일 키인 경우 영구 저장소로 이동 후 CDN URL 생성
        String imageUrl = req.imageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty() && !imageUrl.startsWith("http")) {
            // Local 파일 키로 판단되는 경우 영구 저장소로 이동
            try {
                String permanentKey = localFileService.moveToPermanent(imageUrl, "banner");
                imageUrl = localFileService.getCdnUrl(permanentKey);
            } catch (Exception e) {
                // 이동 실패 시 원본 키로 CDN URL 생성 시도
                imageUrl = localFileService.getCdnUrl(imageUrl);
            }
        }
        
        /* S3 버전 (롤백용 주석처리)
        // 이미지 URL 처리: S3 키인 경우 영구 저장소로 이동 후 CDN URL 생성
        String imageUrl = req.imageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty() && !imageUrl.startsWith("http")) {
            // S3 키로 판단되는 경우 영구 저장소로 이동
            try {
                String permanentKey = awsS3Service.moveToPermanent(imageUrl, "banner");
                imageUrl = awsS3Service.getCdnUrl(permanentKey);
            } catch (Exception e) {
                // 이동 실패 시 원본 키로 CDN URL 생성 시도
                imageUrl = awsS3Service.getCdnUrl(imageUrl);
            }
        }
        */
        final String finalImageUrl = imageUrl;

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
            ps.setString(4, event.getTitleKr());
            ps.setString(5, finalImageUrl);
            ps.setString(6, "https://fair-play.ink/eventdetail/"+event.getEventId());
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
        try {
            String sql = "INSERT INTO banner_application_slot (banner_application_id, slot_id, item_price) VALUES (?, ?, ?)";
            List<Object[]> batchArgs = new ArrayList<>();
            for (int i = 0; i < slotIds.size(); i++) {
                batchArgs.add(new Object[]{appId, slotIds.get(i), prices.get(i)});
            }
            jdbc.batchUpdate(sql, batchArgs);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 이미 신청된 슬롯이 있는 경우
            throw new IllegalStateException("선택한 날짜에 이미 신청된 광고가 있습니다. 다른 날짜를 선택해주세요.", e);
        }

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

        // 3) Payment 엔티티 생성 및 저장
        BannerApplication bannerApplication = bannerApplicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너 신청입니다: " + appId));
        
        PaymentTargetType bannerTargetType = paymentTargetTypeRepository.findByPaymentTargetCode("BANNER_APPLICATION")
                .orElseThrow(() -> new IllegalStateException("BANNER 결제 타입이 존재하지 않습니다"));
        
        PaymentStatusCode paidStatusCode = paymentStatusCodeRepository.findByCode("COMPLETED")
                .orElseThrow(() -> new IllegalStateException("COMPLETED 결제 상태가 존재하지 않습니다"));
        
        PaymentTypeCode bannerPaymentType = paymentTypeCodeRepository.findByCode("CARD")
                .orElseThrow(() -> new IllegalStateException("CARD 결제 타입 코드가 존재하지 않습니다"));
        
        BigDecimal totalAmount = java.math.BigDecimal.valueOf(bannerApplication.getTotalAmount());
        
        Payment payment = Payment.builder()
                .event(bannerApplication.getEvent())
                .user(bannerApplication.getApplicantId())
                .paymentTargetType(bannerTargetType)
                .targetId(appId)
                .merchantUid("banner_" + appId + "_" + System.currentTimeMillis())
                .quantity(1)
                .price(totalAmount)
                .amount(totalAmount) // 총 결제 금액 (price * quantity)
                .paymentTypeCode(bannerPaymentType)
                .paymentStatusCode(paidStatusCode)
                .requestedAt(LocalDateTime.now())
                .paidAt(LocalDateTime.now())
                .build();
        
        Payment savedPayment = paymentRepository.save(payment);
        
        // BannerApplication과 Payment 연결
        bannerApplication.setPayment(savedPayment);
        bannerApplicationRepository.save(bannerApplication);
        
        // 4) 신청 상태 승인 처리
        int updated = jdbc.update("""
                  UPDATE banner_application
                     SET status_code_id = (SELECT apply_status_code_id FROM apply_status_code WHERE code='APPROVED'),
                         approved_by=?, approved_at=NOW()
                   WHERE banner_application_id=? 
                     AND status_code_id = (SELECT apply_status_code_id FROM apply_status_code WHERE code='PENDING')
                """, adminId, appId);
        if (updated != 1) throw new IllegalStateException("신청 상태가 변경되어 승인 처리에 실패했습니다.");
        
        // 5) 승인 완료 이메일 발송
        try {
            sendApprovalEmail(appId, app);
        } catch (Exception e) {
            // 이메일 발송 실패 시 로그만 남기고 처리 계속 진행
            System.err.println("배너 승인 이메일 발송 실패 - appId: " + appId + ", error: " + e.getMessage());
        }
    }

    /** 실제 결제 완료 처리 → Payment 상태 업데이트 + 배너 활성화 */
    @Transactional
    public void completePayment(Long appId, String impUid) {
        // Payment 조회 및 상태 업데이트
        Payment payment = paymentRepository.findByTargetIdAndPaymentTargetType_PaymentTargetCode(appId, "BANNER_APPLICATION")
                .orElseThrow(() -> new IllegalArgumentException("해당 배너 신청의 결제 정보를 찾을 수 없습니다: " + appId));
        
        PaymentStatusCode paidStatusCode = paymentStatusCodeRepository.findByCode("COMPLETED")
                .orElseThrow(() -> new IllegalStateException("PAID 결제 상태가 존재하지 않습니다"));
        
        payment.setPaymentStatusCode(paidStatusCode);
        payment.setImpUid(impUid);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        
        // 배너 슬롯 활성화 (기존 activateBannerSlots 메서드 활용)
        activateBannerSlots(appId);
        
        System.out.println("배너 결제 완료 처리 완료 - appId: " + appId + ", impUid: " + impUid);
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
    /* ===== 관리자 조회용: 목록 ===== */
    public List<AdminApplicationListItemDto> listAdminApplications(String status, String type, Pageable pageable) {
        List<BannerApplication> bannerApplications = bannerApplicationRepository.findAllWithHostInfoOrderByCreatedAtDesc();


//                var params = new MapSqlParameterSource()
//                                .addValue("status", status)
//                                .addValue("type", type)
//                                .addValue("limit", size)
//                                .addValue("offset", page * size);
//
//                        // 1) 신청서 기본 정보
//                                var rows = namedJdbc.query("""
//                SELECT
//                  ba.banner_application_id   AS application_id,
//                  ba.event_id                AS event_id,
//                  ba.title                   AS event_name,
//                  bt.code                    AS banner_type,
//                  ba.created_at              AS applied_at,
//                  asc2.code                  AS apply_status,
//                  ba.image_url               AS image_url,
//                  ba.total_amount            AS total_amount
//                FROM banner_application ba
//                JOIN banner_type bt ON bt.banner_type_id = ba.banner_type_id
//                JOIN apply_status_code asc2 ON asc2.apply_status_code_id = ba.status_code_id
//                WHERE (:status IS NULL OR asc2.code = :status)
//                  AND (:type   IS NULL OR bt.code   = :type)
//                ORDER BY ba.banner_application_id DESC
//                LIMIT :limit OFFSET :offset
//                """, params,
//                                (rs, i) -> Map.of(
//                                                "applicationId", rs.getLong("application_id"),
//                                                "eventId", rs.getLong("event_id"),
//                                                "eventName", rs.getString("event_name"),
//                                                "bannerType", rs.getString("banner_type"),
//                                                "appliedAt", rs.getTimestamp("applied_at").toLocalDateTime(),
//                                                "applyStatus", rs.getString("apply_status"),
//                                                "imageUrl", rs.getString("image_url"),
//                                                "totalAmount", rs.getInt("total_amount")
//                                                ));
//
//                        if (rows.isEmpty()) return List.of();
//
//                        // 2) 슬롯 일괄 조회
//                                var appIds = rows.stream().map(m -> (Long) m.get("applicationId")).toList();
//                var slotMap = fetchSlotsByApplicationIds(appIds);


        // 3) DTO 매핑
        List<AdminApplicationListItemDto> result = new ArrayList<>();
        for (BannerApplication bannerApplication : bannerApplications) {
            List<BannerApplicationSlot> applicationSlots = bannerApplicationSlotRepository.findAllByBannerApplication(bannerApplication);
            List<AdminApplicationSlotDto> slots = new ArrayList<>();
            for (BannerApplicationSlot slot : applicationSlots) {
                AdminApplicationSlotDto dto = AdminApplicationSlotDto.builder()
                        .slotDate(slot.getSlot().getSlotDate())
                        .priority(slot.getSlot().getPriority())
                        .price(slot.getItemPrice())
                        .build();
                slots.add(dto);
            }

            String paymentStatusCode = "PENDING";  // 기본값
            if (bannerApplication.getPayment() != null) {
                Long paymentId = bannerApplication.getPayment().getPaymentId();
                System.out.println("배너 신청 " + bannerApplication.getId() + " - Payment ID: " + paymentId);
                if (bannerApplication.getPayment().getPaymentStatusCode() != null) {
                    paymentStatusCode = bannerApplication.getPayment().getPaymentStatusCode().getCode();
                    System.out.println("배너 신청 " + bannerApplication.getId() + " - Payment 상태: " + paymentStatusCode);
                } else {
                    System.out.println("배너 신청 " + bannerApplication.getId() + " - Payment 존재하지만 PaymentStatusCode가 null");
                }
            } else {
                System.out.println("배너 신청 " + bannerApplication.getId() + " - Payment가 null");
                
                // 직접 DB에서 Payment 조회해보기
                try {
                    Payment directPayment = paymentRepository.findByTargetIdAndPaymentTargetType_PaymentTargetCode(
                            bannerApplication.getId(), "BANNER_APPLICATION").orElse(null);
                    if (directPayment != null) {
                        System.out.println("배너 신청 " + bannerApplication.getId() + " - 직접 조회된 Payment ID: " + 
                                directPayment.getPaymentId() + ", 상태: " + 
                                (directPayment.getPaymentStatusCode() != null ? directPayment.getPaymentStatusCode().getCode() : "NULL"));
                        paymentStatusCode = directPayment.getPaymentStatusCode().getCode();
                    } else {
                        System.out.println("배너 신청 " + bannerApplication.getId() + " - 직접 조회해도 Payment 없음");
                    }
                } catch (Exception e) {
                    System.out.println("배너 신청 " + bannerApplication.getId() + " - Payment 직접 조회 실패: " + e.getMessage());
                }
            }

            // 호스트명을 올바르게 가져오기 (Event → EventAdmin → Users)
            String hostName = "";
            if (bannerApplication.getEvent() != null && 
                bannerApplication.getEvent().getManager() != null && 
                bannerApplication.getEvent().getManager().getUser() != null) {
                hostName = bannerApplication.getEvent().getManager().getUser().getName();
            }

            result.add(new AdminApplicationListItemDto(
                    bannerApplication.getId(),
                    hostName,
                    bannerApplication.getEvent().getEventId(),
                    bannerApplication.getEvent().getTitleKr(),
                    bannerApplication.getBannerType().getCode(),
                    bannerApplication.getCreatedAt(),
                    bannerApplication.getStatusCode().getCode(),
                    paymentStatusCode,
                    bannerApplication.getImageUrl(),
                    bannerApplication.getTotalAmount(),
                    slots
            ));
        }
        return result;
    }

            /* ===== 관리자 조회용: 단건 뷰 ===== */
            public AdminApplicationListItemDto getAdminApplicationView(Long appId) {
                Map<String, Object> m = namedJdbc.queryForObject("""
                SELECT
                  ba.banner_application_id   AS application_id,
                  ba.event_id                AS event_id,
                  ba.title                   AS event_name,
                  bt.code                    AS banner_type,
                  ba.created_at              AS applied_at,
                  asc2.code                  AS apply_status,
                  ba.image_url               AS image_url,
                  ba.total_amount            AS total_amount
                FROM banner_application ba
                JOIN banner_type bt ON bt.banner_type_id = ba.banner_type_id
                JOIN apply_status_code asc2 ON asc2.apply_status_code_id = ba.status_code_id
                WHERE ba.banner_application_id = :id
                """, new MapSqlParameterSource("id", appId),
                                (rs, i) -> Map.of(
                                                "applicationId", rs.getLong("application_id"),
                                                "eventId", rs.getLong("event_id"),
                                                "eventName", rs.getString("event_name"),
                                                "bannerType", rs.getString("banner_type"),
                                                "appliedAt", rs.getTimestamp("applied_at").toLocalDateTime(),
                                                "applyStatus", rs.getString("apply_status"),
                                                "imageUrl", rs.getString("image_url"),
                                                "totalAmount", rs.getInt("total_amount")
                                                ));

                        var slotMap = fetchSlotsByApplicationIds(List.of(appId));
                return new AdminApplicationListItemDto(
                                (Long) m.get("applicationId"),
                                "",
                                (Long) m.get("eventId"),
                                (String) m.get("eventName"),
                                (String) m.get("bannerType"),
                                (LocalDateTime) m.get("appliedAt"),
                                (String) m.get("applyStatus"),
                                mapPaymentStatus((String) m.get("applyStatus")),
                                (String) m.get("imageUrl"),
                                (Integer) m.get("totalAmount"),
                                slotMap.getOrDefault(appId, List.of())
                               );
            }

            private Map<Long, List<AdminApplicationSlotDto>> fetchSlotsByApplicationIds(List<Long> appIds) {
                if (appIds.isEmpty()) return Map.of();
                var rows = namedJdbc.query("""
                SELECT bas.banner_application_id AS application_id,
                       s.slot_date                AS slot_date,
                       s.priority                 AS priority,
                       bas.item_price             AS price
                FROM banner_application_slot bas
                JOIN banner_slot s ON s.slot_id = bas.slot_id
                WHERE bas.banner_application_id IN (:ids)
                ORDER BY s.slot_date, s.priority
                """, new MapSqlParameterSource("ids", appIds),
                                (rs, i) -> Map.of(
                                               "applicationId", rs.getLong("application_id"),
                                               "slotDate", rs.getDate("slot_date").toLocalDate(),
                                               "priority", rs.getInt("priority"),
                                               "price", rs.getInt("price")
                                               ));
                Map<Long, List<AdminApplicationSlotDto>> map = new HashMap<>();
                for (var r : rows) {
                        Long appId = (Long) r.get("applicationId");
                        map.computeIfAbsent(appId, k -> new ArrayList<>())
                                   .add(new AdminApplicationSlotDto(
                                           (LocalDate) r.get("slotDate"),
                                           (Integer) r.get("priority"),
                                           (Integer) r.get("price")));
                    }
                return map;
            }

            // 간단한 결제상태 표기: 실제 결제 테이블 연동 전 임시
            private String mapPaymentStatus(String applyStatus) {
                return switch (applyStatus) {
                        case "APPROVED" -> "PAID";
                        case "REJECTED" -> "N/A";
                        default -> "WAITING";
                    };
            }

            /**
             * 배너 승인 이메일 발송
             * 부스와 달리 배너는 승인과 동시에 결제 완료 처리되므로 결제 링크가 아닌 완료 알림
             */
            private void sendApprovalEmail(Long appId, Map<String, Object> appInfo) {
                try {
                    // 신청자 정보 및 이벤트 관리자 정보 조회
                    Map<String, Object> applicantInfo = jdbc.queryForMap("""
                        SELECT ba.applicant_id, ba.title, ba.total_amount, ba.event_id,
                               bt.name as banner_type_name,
                               u.name as applicant_name,
                               ea.contact_email as event_admin_email
                        FROM banner_application ba
                        JOIN banner_type bt ON bt.banner_type_id = ba.banner_type_id  
                        JOIN users u ON u.user_id = ba.applicant_id
                        JOIN event e ON e.event_id = ba.event_id
                        JOIN event_admin ea ON ea.user_id = e.manager_id
                        WHERE ba.banner_application_id = ?
                        """, appId);
                    
                    String eventAdminEmail = (String) applicantInfo.get("event_admin_email");
                    String applicantName = (String) applicantInfo.get("applicant_name");
                    String title = (String) applicantInfo.get("title");
                    String bannerType = (String) applicantInfo.get("banner_type_name");
                    Integer totalAmount = (Integer) applicantInfo.get("total_amount");

                    // 배너는 승인과 동시에 결제 완료되므로 결제 링크 대신 완료 알림 발송
                    bannerEmailService.sendApprovalWithPaymentEmail(
                        eventAdminEmail,
                        title,
                        bannerType,
                        totalAmount,
                        appId,
                        applicantName
                    );

                    System.out.println("배너 승인 이메일 발송 완료 - appId: " + appId +
                                     ", recipient: " + eventAdminEmail);
                    
                } catch (Exception e) {
                    System.err.println("배너 승인 이메일 발송 중 오류 - appId: " + appId + 
                                     ", error: " + e.getMessage());
                    throw e;
                }
            }

            /**
             * 배너 신청 승인 (부스 방식처럼 승인 후 결제 링크 이메일 발송)
             * 기존 markPaid와 달리 승인만 처리하고 결제는 별도로 진행
             */
            @Transactional
            public void approveWithPaymentLink(Long appId, Long adminId) {
                try {
                    // 1. 신청 상태 확인
                    Integer pendingId = statusId(STATUS_PENDING);
                    Integer appStatus = jdbc.queryForObject(
                            "SELECT status_code_id FROM banner_application WHERE banner_application_id=? FOR UPDATE",
                            Integer.class, appId
                    );
                    if (!Objects.equals(appStatus, pendingId)) {
                        throw new IllegalStateException("승인할 수 없는 상태입니다.");
                    }

                    // 2. Payment 엔티티 생성 (PENDING 상태)
                    BannerApplication bannerApplication = bannerApplicationRepository.findById(appId)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배너 신청입니다: " + appId));
                    
                    PaymentTargetType bannerTargetType = paymentTargetTypeRepository.findByPaymentTargetCode("BANNER_APPLICATION")
                            .orElseThrow(() -> new IllegalStateException("BANNER 결제 타입이 존재하지 않습니다"));
                    
                    PaymentStatusCode pendingPaymentStatus = paymentStatusCodeRepository.findByCode("PENDING")
                            .orElseThrow(() -> new IllegalStateException("PENDING 결제 상태가 존재하지 않습니다"));

                    PaymentTypeCode bannerPaymentType = paymentTypeCodeRepository.findByCode("CARD")
                            .orElseThrow(() -> new IllegalStateException("CARD 결제 타입 코드가 존재하지 않습니다"));

                    BigDecimal totalAmount = java.math.BigDecimal.valueOf(bannerApplication.getTotalAmount());
                    
                    Payment payment = Payment.builder()
                            .event(bannerApplication.getEvent())
                            .user(bannerApplication.getApplicantId())
                            .paymentTargetType(bannerTargetType)
                            .targetId(appId)
                            .merchantUid("banner_" + appId + "_" + System.currentTimeMillis())
                            .quantity(1)
                            .price(totalAmount)
                            .amount(totalAmount) // 총 결제 금액 (price * quantity)
                            .paymentTypeCode(bannerPaymentType)
                            .paymentStatusCode(pendingPaymentStatus)
                            .requestedAt(LocalDateTime.now())
                            .build();
                    
                    Payment savedPayment = paymentRepository.save(payment);
                    
                    // BannerApplication과 Payment 연결
                    bannerApplication.setPayment(savedPayment);


                    // 3. 신청 상태를 APPROVED로 변경 (결제는 별도)
                    ApplyStatusCode applyStatusCode = applyStatusCodeRepository.findByCode("APPROVED")
                            .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "승인 신청 상태를 찾을 수 없습니다."));
                    bannerApplication.setStatusCode(applyStatusCode);

                    bannerApplicationRepository.save(bannerApplication);

                    // 4. 결제 링크가 포함된 승인 이메일 발송
                    sendApprovalEmailWithPaymentLink(appId);
                    
                    System.out.println("배너 승인 완료 (결제 링크 발송) - appId: " + appId);

                } catch (Exception e) {
                    System.err.println("배너 승인 처리 실패 - appId: " + appId + ", error: " + e.getMessage());
                    throw e;
                }
            }

            /**
             * 승인 시 결제 링크가 포함된 이메일 발송
             */
            private void sendApprovalEmailWithPaymentLink(Long appId) {
                try {
                    // 신청자 정보 및 이벤트 관리자 정보 조회
                    Map<String, Object> applicantInfo = jdbc.queryForMap("""
                        SELECT ba.applicant_id, ba.title, ba.total_amount, ba.event_id,
                               bt.name as banner_type_name,
                               u.name as applicant_name,
                               ea.contact_email as event_admin_email
                        FROM banner_application ba
                        JOIN banner_type bt ON bt.banner_type_id = ba.banner_type_id  
                        JOIN users u ON u.user_id = ba.applicant_id
                        JOIN event e ON e.event_id = ba.event_id
                        JOIN event_admin ea ON ea.user_id = e.manager_id
                        WHERE ba.banner_application_id = ?
                        """, appId);
                    
                    String eventAdminEmail = (String) applicantInfo.get("event_admin_email");
                    String applicantName = (String) applicantInfo.get("applicant_name");
                    String title = (String) applicantInfo.get("title");
                    String bannerType = (String) applicantInfo.get("banner_type_name");
                    Integer totalAmount = (Integer) applicantInfo.get("total_amount");

                    // 부스와 동일한 방식으로 결제 링크가 포함된 이메일 발송
                    bannerEmailService.sendApprovalWithPaymentEmail(
                        eventAdminEmail,
                        title,
                        bannerType,
                        totalAmount,
                        appId,
                        applicantName
                    );

                    System.out.println("배너 승인 이메일 발송 완료 (결제 링크 포함) - appId: " + appId +
                                     ", recipient: " + eventAdminEmail);
                    
                } catch (Exception e) {
                    System.err.println("배너 승인 이메일 발송 실패 - appId: " + appId + 
                                     ", error: " + e.getMessage());
                    throw e;
                }
            }

            /**
             * 결제 완료 시 배너 슬롯 활성화 (부스 방식처럼 승인과 결제를 분리할 때 사용)
             * 현재는 markPaid에서 승인과 결제를 동시에 처리하지만, 
             * 향후 부스처럼 승인 → 결제 → 활성화 순서로 변경할 때를 대비
             */
            public void activateBannerSlots(Long appId) {
                try {
                    // 배너 신청 정보 조회
                    Map<String, Object> app = jdbc.queryForMap("""
                        SELECT event_id, title, image_url, link_url, banner_type_id
                        FROM banner_application WHERE banner_application_id=?
                        """, appId);
                    
                    // 해당 신청의 슬롯들을 SOLD로 변경하고 배너 생성
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
                    
                    if (slots.isEmpty()) {
                        System.out.println("활성화할 슬롯이 없습니다 - appId: " + appId);
                        return;
                    }
                    
                    // 슬롯들을 SOLD로 변경
                    var slotIdList = slots.stream()
                            .map(m -> (Long) m.get("slotId"))
                            .toList();
                    
                    var soldParams = new MapSqlParameterSource()
                            .addValue("slotIds", slotIdList);
                            
                    int sold = namedJdbc.update("""
                        UPDATE banner_slot
                           SET status='SOLD'
                         WHERE slot_id IN (:slotIds)
                           AND status IN ('LOCKED', 'AVAILABLE')
                        """, soldParams);
                        
                    // 배너 상태 코드 조회 (ACTIVE) - 루프 밖에서 한 번만 조회
                    Integer activeStatusCodeId = jdbc.queryForObject("""
                        SELECT banner_status_code_id FROM banner_status_code WHERE code = 'ACTIVE'
                        """, Integer.class);

                    // 각 슬롯에 대해 배너 생성
                    for (var slot : slots) {
                        Long bannerId = null;
                        
                        try {
                            // 먼저 기존 배너가 있는지 확인
                            List<Long> existingBanners = jdbc.queryForList("""
                                SELECT banner_id FROM banner 
                                WHERE banner_type_id = ? AND priority = ? 
                                AND start_date = (SELECT start_date FROM banner_application WHERE banner_application_id = ?)
                                AND end_date = (SELECT end_date FROM banner_application WHERE banner_application_id = ?)
                                """, Long.class, 
                                slot.get("typeId"), slot.get("priority"), appId, appId);
                            
                            if (!existingBanners.isEmpty()) {
                                // 기존 배너가 있으면 첫 번째 것을 재사용
                                bannerId = existingBanners.get(0);
                                System.out.println("기존 배너 재사용 - bannerId: " + bannerId + ", typeId: " + slot.get("typeId") + ", priority: " + slot.get("priority"));
                                
                                // 기존 배너 정보를 업데이트
                                jdbc.update("""
                                    UPDATE banner SET 
                                        event_id = ?, title = ?, image_url = ?, link_url = ?, 
                                        banner_status_code_id = ?, created_at = NOW()
                                    WHERE banner_id = ?
                                    """, 
                                    app.get("event_id"), app.get("title"), app.get("image_url"), 
                                    app.get("link_url"), activeStatusCodeId, bannerId);
                            } else {
                                // 새 배너 생성
                                KeyHolder keyHolder = new GeneratedKeyHolder();
                                jdbc.update(con -> {
                                    PreparedStatement ps = con.prepareStatement("""
                                        INSERT INTO banner (event_id, title, image_url, link_url, banner_type_id, 
                                                           priority, start_date, end_date, banner_status_code_id, created_at)
                                        SELECT ?, ?, ?, ?, ?, ?, 
                                               (SELECT start_date FROM banner_application WHERE banner_application_id = ?),
                                               (SELECT end_date FROM banner_application WHERE banner_application_id = ?),
                                               ?, NOW()
                                        """, Statement.RETURN_GENERATED_KEYS);
                                    ps.setObject(1, app.get("event_id"));
                                    ps.setObject(2, app.get("title"));
                                    ps.setObject(3, app.get("image_url"));
                                    ps.setObject(4, app.get("link_url"));
                                    ps.setObject(5, slot.get("typeId"));
                                    ps.setObject(6, slot.get("priority"));
                                    ps.setLong(7, appId);
                                    ps.setLong(8, appId);
                                    ps.setInt(9, activeStatusCodeId);
                                    return ps;
                                }, keyHolder);

                                bannerId = Objects.requireNonNull(keyHolder.getKey()).longValue();
                                System.out.println("새 배너 생성 - bannerId: " + bannerId + ", typeId: " + slot.get("typeId") + ", priority: " + slot.get("priority"));
                            }
                            
                            // 슬롯에 배너 ID 연결
                            jdbc.update("""
                                UPDATE banner_slot SET sold_banner_id = ? WHERE slot_id = ?
                                """, bannerId, slot.get("slotId"));
                                
                        } catch (Exception e) {
                            System.out.println("배너 생성/업데이트 실패 - slot: " + slot + ", error: " + e.getMessage());
                            throw e;
                        }
                    }

                    System.out.println("배너 슬롯 활성화 및 배너 생성 완료 - appId: " + appId +
                                     ", 활성화된 슬롯 수: " + sold + ", 생성된 배너 수: " + slots.size());
                    
                } catch (Exception e) {
                    System.err.println("배너 슬롯 활성화 실패 - appId: " + appId + 
                                     ", error: " + e.getMessage());
                }
            }
    }