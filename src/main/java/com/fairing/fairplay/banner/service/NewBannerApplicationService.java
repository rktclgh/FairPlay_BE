package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.*;
import com.fairing.fairplay.banner.entity.*;
import com.fairing.fairplay.banner.repository.*;
import com.fairing.fairplay.core.email.service.BannerEmailService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NewBannerApplicationService {
    
    private final NewBannerApplicationRepository applicationRepository;
    private final NewBannerSlotRepository slotRepository;
    private final NewBannerTypeRepository typeRepository;
    private final NewBannerLogRepository logRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final BannerEmailService emailService;

    /**
     * 배너 신청서 생성 (새로운 단순한 방식)
     * 슬롯을 미리 생성하지 않고, 신청서만 생성
     */
    public Long createApplication(NewBannerApplicationRequestDto dto, Long userId) {
        log.info("새로운 배너 신청서 생성 - 사용자: {}, 이벤트: {}", userId, dto.getEventId());
        
        // 1. 사용자 및 이벤트 검증
        Users applicant = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Event event = eventRepository.findById(dto.getEventId())
            .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));
        NewBannerType bannerType = typeRepository.findByCode(dto.getBannerType())
            .orElseThrow(() -> new IllegalArgumentException("배너 타입을 찾을 수 없습니다."));

        // 2. 기간 유효성 검사
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다.");
        }

        // 3. 총 금액 계산
        long totalDays = dto.getStartDate().until(dto.getEndDate()).getDays() + 1;
        int totalAmount = (int) (bannerType.getBasePrice() * totalDays);

        // 4. 신청서 생성
        NewBannerApplication application = NewBannerApplication.builder()
            .eventId(dto.getEventId())
            .applicantId(userId)
            .bannerTypeId(bannerType.getId())
            .title(dto.getTitle())
            .imageUrl(dto.getImageUrl())
            .linkUrl(dto.getLinkUrl())
            .startDate(dto.getStartDate().atStartOfDay())
            .endDate(dto.getEndDate().atTime(23, 59, 59))
            .totalAmount(totalAmount)
            .applicationStatus(NewBannerApplication.ApplicationStatus.PENDING)
            .paymentStatus(NewBannerApplication.PaymentStatus.PENDING)
            .build();

        NewBannerApplication saved = applicationRepository.save(application);

        // 5. 로그 기록
        logActivity(saved.getId(), "CREATED", null, "PENDING", userId, "배너 신청서가 생성되었습니다.");

        log.info("배너 신청서 생성 완료 - ID: {}, 총 금액: {}원", saved.getId(), totalAmount);
        return saved.getId();
    }

    /**
     * 배너 신청서 승인 (이메일 발송)
     */
    public void approveApplication(Long applicationId, Long adminId, String comment) {
        log.info("배너 신청서 승인 - ID: {}, 승인자: {}", applicationId, adminId);
        
        NewBannerApplication application = getApplication(applicationId);
        Users admin = userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        if (application.getApplicationStatus() != NewBannerApplication.ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기 상태인 신청서만 승인할 수 있습니다.");
        }

        // 1. 상태 업데이트
        application.setApplicationStatus(NewBannerApplication.ApplicationStatus.APPROVED);
        application.setApprovedBy(adminId);
        application.setApprovedAt(LocalDateTime.now());
        application.setAdminComment(comment);

        // 2. 이메일 발송
        Users applicant = userRepository.findById(application.getApplicantId())
            .orElseThrow(() -> new IllegalArgumentException("신청자를 찾을 수 없습니다."));
        NewBannerType bannerType = typeRepository.findById(application.getBannerTypeId())
            .orElseThrow(() -> new IllegalArgumentException("배너 타입을 찾을 수 없습니다."));

        emailService.sendApprovalWithPaymentEmail(
            applicant.getEmail(),
            application.getTitle(),
            bannerType.getName(),
            application.getTotalAmount(),
            applicationId,
            applicant.getName()
        );

        // 3. 로그 기록
        logActivity(applicationId, "APPROVED", "PENDING", "APPROVED", adminId, 
            "배너 신청서가 승인되었습니다. " + (comment != null ? "코멘트: " + comment : ""));

        log.info("배너 신청서 승인 완료 - ID: {}", applicationId);
    }

    /**
     * 배너 신청서 반려
     */
    public void rejectApplication(Long applicationId, Long adminId, String reason) {
        log.info("배너 신청서 반려 - ID: {}, 반려자: {}", applicationId, adminId);
        
        NewBannerApplication application = getApplication(applicationId);

        if (application.getApplicationStatus() != NewBannerApplication.ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기 상태인 신청서만 반려할 수 있습니다.");
        }

        // 1. 상태 업데이트
        application.setApplicationStatus(NewBannerApplication.ApplicationStatus.REJECTED);
        application.setAdminComment(reason);

        // 2. 반려 이메일 발송
        Users applicant = userRepository.findById(application.getApplicantId())
            .orElseThrow(() -> new IllegalArgumentException("신청자를 찾을 수 없습니다."));

        emailService.sendRejectionEmail(
            applicant.getEmail(),
            application.getTitle(),
            reason != null ? reason : "승인 기준에 부합하지 않습니다."
        );

        // 3. 로그 기록
        logActivity(applicationId, "REJECTED", "PENDING", "REJECTED", adminId, 
            "배너 신청서가 반려되었습니다. 사유: " + reason);

        log.info("배너 신청서 반려 완료 - ID: {}", applicationId);
    }

    /**
     * 결제 완료 처리 및 슬롯 생성 (핵심 로직)
     * 기존의 복잡한 슬롯 잠금 대신 단순한 슬롯 생성
     */
    public void completePayment(Long applicationId, String paymentId) {
        log.info("배너 결제 완료 처리 - ID: {}, 결제 ID: {}", applicationId, paymentId);
        
        NewBannerApplication application = getApplication(applicationId);

        if (application.getApplicationStatus() != NewBannerApplication.ApplicationStatus.APPROVED) {
            throw new IllegalStateException("승인된 신청서만 결제 처리할 수 있습니다.");
        }

        if (application.getPaymentStatus() == NewBannerApplication.PaymentStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 신청서입니다.");
        }

        // 1. 결제 상태 업데이트
        application.setPaymentStatus(NewBannerApplication.PaymentStatus.PAID);
        application.setPaidAt(LocalDateTime.now());

        // 2. 날짜별 슬롯 생성 (핵심: 신청 기반으로 슬롯 생성)
        List<LocalDate> dateRange = getDateRange(
            application.getStartDate().toLocalDate(), 
            application.getEndDate().toLocalDate()
        );

        int dailyPrice = (int) (application.getTotalAmount() / dateRange.size());

        for (LocalDate date : dateRange) {
            NewBannerSlot slot = NewBannerSlot.builder()
                .bannerApplicationId(applicationId)
                .slotDate(date)
                .priority(1) // 기본 우선순위
                .price(dailyPrice)
                .status(NewBannerSlot.Status.ACTIVE) // 바로 활성화
                .activatedAt(LocalDateTime.now())
                .build();
            
            slotRepository.save(slot);
        }

        // 3. 로그 기록
        logActivity(applicationId, "PAYMENT_COMPLETED", "PENDING", "PAID", null, 
            String.format("결제가 완료되어 %d개의 슬롯이 생성되었습니다. 결제 ID: %s", 
                dateRange.size(), paymentId));

        log.info("배너 결제 완료 및 슬롯 생성 완료 - ID: {}, 슬롯 개수: {}", applicationId, dateRange.size());
    }

    /**
     * 호스트용 배너 신청서 목록 조회 (필터링 포함)
     */
    @Transactional(readOnly = true)
    public Page<BannerApplicationDto> getHostApplications(Long userId, String status, 
                                                         String bannerTypeCode, 
                                                         LocalDate startDate, LocalDate endDate, 
                                                         Pageable pageable) {
        
        return applicationRepository.findHostApplicationsWithFilter(
            userId, status, bannerTypeCode, startDate, endDate, pageable
        ).map(this::convertToDto);
    }

    /**
     * 관리자용 배너 신청서 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminApplicationListItemDto> getAdminApplications(String status, String bannerTypeCode, 
                                                                Pageable pageable) {
        return applicationRepository.findAdminApplicationsWithFilter(status, bannerTypeCode, pageable)
            .map(this::convertToAdminDto);
    }

    /**
     * 배너 신청서 하드 삭제 (관리자 전용)
     */
    public void hardDeleteApplication(Long applicationId, Long adminId, String reason) {
        log.warn("배너 신청서 하드 삭제 - ID: {}, 삭제자: {}, 사유: {}", applicationId, adminId, reason);
        
        NewBannerApplication application = getApplication(applicationId);

        // 1. 연관된 슬롯 삭제
        slotRepository.deleteByBannerApplicationId(applicationId);

        // 2. 삭제 로그 기록
        logActivity(applicationId, "HARD_DELETED", 
            application.getApplicationStatus().toString(), "DELETED", adminId, 
            "관리자에 의해 완전 삭제되었습니다. 사유: " + reason);

        // 3. 신청서 삭제
        applicationRepository.delete(application);

        log.warn("배너 신청서 하드 삭제 완료 - ID: {}", applicationId);
    }

    // === Private Helper Methods ===

    private NewBannerApplication getApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("배너 신청서를 찾을 수 없습니다. ID: " + applicationId));
    }

    private List<LocalDate> getDateRange(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
            .collect(Collectors.toList());
    }

    private void logActivity(Long applicationId, String actionType, String oldStatus, 
                           String newStatus, Long adminId, String comment) {
        NewBannerLog log = NewBannerLog.builder()
            .bannerApplicationId(applicationId)
            .actionType(actionType)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .adminId(adminId)
            .comment(comment)
            .build();
        
        logRepository.save(log);
    }

    private BannerApplicationDto convertToDto(NewBannerApplication application) {
        NewBannerType bannerType = typeRepository.findById(application.getBannerTypeId()).orElse(null);
        Event event = eventRepository.findById(application.getEventId()).orElse(null);
        
        return BannerApplicationDto.builder()
            .id(application.getId())
            .eventTitle(event != null ? event.getTitleKr() : "")
            .bannerType(bannerType != null ? bannerType.getCode() : "")
            .bannerTypeName(bannerType != null ? bannerType.getName() : "")
            .title(application.getTitle())
            .imageUrl(application.getImageUrl())
            .linkUrl(application.getLinkUrl())
            .startDate(application.getStartDate().toString())
            .endDate(application.getEndDate().toString())
            .totalAmount(application.getTotalAmount())
            .applicationStatus(application.getApplicationStatus())
            .paymentStatus(application.getPaymentStatus())
            .combinedStatus(getCombinedStatus(application))
            .createdAt(application.getCreatedAt())
            .approvedAt(application.getApprovedAt())
            .paidAt(application.getPaidAt())
            .adminComment(application.getAdminComment())
            .canCancel(canCancel(application))
            .canPay(canPay(application))
            .paymentUrl(getPaymentUrl(application))
            .build();
    }

    private AdminApplicationListItemDto convertToAdminDto(NewBannerApplication application) {
        // 관리자용 DTO 변환 로직
        return AdminApplicationListItemDto.builder()
            .applicationId(application.getId())
            .hostName(getApplicantName(application.getApplicantId()))
            .eventId(application.getEventId())
            .eventName(getEventTitle(application.getEventId()))
            .bannerType(getBannerTypeCode(application.getBannerTypeId()))
            .appliedAt(application.getCreatedAt())
            .applyStatus(application.getApplicationStatus().toString())
            .paymentStatus(application.getPaymentStatus().toString())
            .imageUrl(application.getImageUrl())
            .totalAmount(application.getTotalAmount())
            .build();
    }

    private String getCombinedStatus(NewBannerApplication application) {
        if (application.getPaymentStatus() == NewBannerApplication.PaymentStatus.PAID) {
            return "결제 완료";
        }
        if (application.getApplicationStatus() == NewBannerApplication.ApplicationStatus.APPROVED) {
            return "결제 대기";
        }
        if (application.getApplicationStatus() == NewBannerApplication.ApplicationStatus.REJECTED) {
            return "반려됨";
        }
        return "승인 대기";
    }

    private boolean canCancel(NewBannerApplication application) {
        return application.getApplicationStatus() == NewBannerApplication.ApplicationStatus.PENDING ||
               (application.getApplicationStatus() == NewBannerApplication.ApplicationStatus.APPROVED && 
                application.getPaymentStatus() != NewBannerApplication.PaymentStatus.PAID);
    }

    private boolean canPay(NewBannerApplication application) {
        return application.getApplicationStatus() == NewBannerApplication.ApplicationStatus.APPROVED &&
               application.getPaymentStatus() == NewBannerApplication.PaymentStatus.PENDING;
    }

    private String getPaymentUrl(NewBannerApplication application) {
        if (canPay(application)) {
            return "https://fair-play.ink/banner/payment?applicationId=" + application.getId();
        }
        return null;
    }

    private String getEventTitle(Long eventId) {
        return eventRepository.findById(eventId)
            .map(Event::getTitleKr)
            .orElse("");
    }

    private String getApplicantName(Long applicantId) {
        return userRepository.findById(applicantId)
            .map(Users::getName)
            .orElse("");
    }

    private String getBannerTypeCode(Long bannerTypeId) {
        return typeRepository.findById(bannerTypeId)
            .map(NewBannerType::getCode)
            .orElse("");
    }

    /**
     * 신청서 상세 조회 (호스트용)
     */
    @Transactional(readOnly = true)
    public BannerApplicationDto getApplicationDetail(Long applicationId, Long userId) {
        NewBannerApplication application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("배너 신청서를 찾을 수 없습니다."));
        
        // 본인 신청서인지 확인
        if (!application.getApplicantId().equals(userId)) {
            throw new IllegalArgumentException("본인의 신청서만 조회할 수 있습니다.");
        }
        
        return convertToDto(application);
    }

    /**
     * 신청서 취소 (호스트용)
     */
    public void cancelApplication(Long applicationId, Long userId, String reason) {
        log.info("배너 신청서 취소 - ID: {}, 취소자: {}, 사유: {}", applicationId, userId, reason);
        
        NewBannerApplication application = getApplication(applicationId);
        
        // 본인 신청서인지 확인
        if (!application.getApplicantId().equals(userId)) {
            throw new IllegalArgumentException("본인의 신청서만 취소할 수 있습니다.");
        }
        
        // 취소 가능 상태인지 확인
        if (!canCancel(application)) {
            throw new IllegalStateException("현재 상태에서는 취소할 수 없습니다.");
        }
        
        // 상태 업데이트
        application.setApplicationStatus(NewBannerApplication.ApplicationStatus.CANCELLED);
        application.setAdminComment(reason);
        
        // 로그 기록
        logActivity(applicationId, "CANCELLED", 
            application.getApplicationStatus().toString(), "CANCELLED", userId, 
            "호스트에 의해 취소되었습니다. 사유: " + reason);
        
        log.info("배너 신청서 취소 완료 - ID: {}", applicationId);
    }

    /**
     * 호스트 통계 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getHostStatistics(Long userId) {
        List<NewBannerApplication> applications = applicationRepository.findByApplicantId(userId);
        
        long totalApplications = applications.size();
        long pendingApplications = applications.stream()
            .filter(app -> app.getApplicationStatus() == NewBannerApplication.ApplicationStatus.PENDING)
            .count();
        long approvedApplications = applications.stream()
            .filter(app -> app.getApplicationStatus() == NewBannerApplication.ApplicationStatus.APPROVED)
            .count();
        long paidApplications = applications.stream()
            .filter(app -> app.getPaymentStatus() == NewBannerApplication.PaymentStatus.PAID)
            .count();
        long rejectedApplications = applications.stream()
            .filter(app -> app.getApplicationStatus() == NewBannerApplication.ApplicationStatus.REJECTED)
            .count();
        long totalSpent = applications.stream()
            .filter(app -> app.getPaymentStatus() == NewBannerApplication.PaymentStatus.PAID)
            .mapToLong(NewBannerApplication::getTotalAmount)
            .sum();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalApplications", totalApplications);
        stats.put("pendingApplications", pendingApplications);
        stats.put("approvedApplications", approvedApplications);
        stats.put("paidApplications", paidApplications);
        stats.put("rejectedApplications", rejectedApplications);
        stats.put("totalSpent", totalSpent);
        
        return stats;
    }

    /**
     * 배너 타입 목록 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getBannerTypes() {
        List<NewBannerType> types = typeRepository.findAll();
        
        Map<String, Map<String, Object>> typesMap = new HashMap<>();
        for (NewBannerType type : types) {
            Map<String, Object> typeInfo = new HashMap<>();
            typeInfo.put("name", type.getName());
            typeInfo.put("basePrice", type.getBasePrice());
            typeInfo.put("description", type.getDescription());
            typesMap.put(type.getCode(), typeInfo);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("types", typesMap);
        
        return result;
    }

    /**
     * 슬롯 가용성 조회 (가상 슬롯 정보 반환)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableSlots(String typeCode, String from, String to) {
        LocalDate startDate = LocalDate.parse(from);
        LocalDate endDate = LocalDate.parse(to);
        
        List<Map<String, Object>> slots = new ArrayList<>();
        
        // 해당 기간의 실제 예약된 슬롯 조회
        List<NewBannerSlot> existingSlots = slotRepository.findBySlotDateBetween(startDate, endDate);
        
        // 날짜별로 그룹핑
        Map<LocalDate, List<NewBannerSlot>> slotsByDate = existingSlots.stream()
            .collect(Collectors.groupingBy(NewBannerSlot::getSlotDate));
        
        // 각 날짜에 대해 슬롯 정보 생성
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            List<NewBannerSlot> daySlots = slotsByDate.getOrDefault(current, new ArrayList<>());
            
            if ("HERO".equals(typeCode)) {
                // HERO 타입: 1~10 순위별 슬롯 생성
                for (int priority = 1; priority <= 10; priority++) {
                    final int p = priority;
                    boolean isOccupied = daySlots.stream().anyMatch(slot -> slot.getPriority() == p);
                    
                    Map<String, Object> slot = new HashMap<>();
                    slot.put("slotDate", current.toString());
                    slot.put("priority", priority);
                    slot.put("status", isOccupied ? "SOLD" : "AVAILABLE");
                    slot.put("price", getPriceForHeroRank(priority));
                    
                    slots.add(slot);
                }
            } else if ("SEARCH_TOP".equals(typeCode)) {
                // SEARCH_TOP 타입: 최대 2개 우선순위
                for (int priority = 1; priority <= 2; priority++) {
                    final int p = priority;
                    boolean isOccupied = daySlots.stream().anyMatch(slot -> slot.getPriority() == p);
                    
                    Map<String, Object> slot = new HashMap<>();
                    slot.put("slotDate", current.toString());
                    slot.put("priority", priority);
                    slot.put("status", isOccupied ? "SOLD" : "AVAILABLE");
                    slot.put("price", 500000); // MD PICK은 50만원/일
                    
                    slots.add(slot);
                }
            }
            
            current = current.plusDays(1);
        }
        
        return slots;
    }

    /**
     * 새로운 신청서 생성 (Map 요청 처리)
     */
    public Long createApplication(Long userId, Map<String, Object> requestBody) {
        log.info("배너 신청서 생성 - 사용자: {}, 요청: {}", userId, requestBody);
        
        try {
            // 요청 데이터 파싱
            Long eventId = getLongValue(requestBody, "eventId");
            String bannerType = (String) requestBody.get("bannerType");
            String title = (String) requestBody.get("title");
            String imageUrl = (String) requestBody.get("imageUrl");
            String linkUrl = (String) requestBody.get("linkUrl");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) requestBody.get("items");
            
            // 날짜 범위 계산
            LocalDate minDate = null;
            LocalDate maxDate = null;
            long totalAmount = 0;
            
            for (Map<String, Object> item : items) {
                String dateStr = (String) item.get("date");
                Integer priority = (Integer) item.get("priority");
                
                LocalDate date = LocalDate.parse(dateStr);
                if (minDate == null || date.isBefore(minDate)) minDate = date;
                if (maxDate == null || date.isAfter(maxDate)) maxDate = date;
                
                // 가격 계산
                if ("HERO".equals(bannerType)) {
                    totalAmount += getPriceForHeroRank(priority);
                } else if ("SEARCH_TOP".equals(bannerType)) {
                    totalAmount += 500000;
                }
            }
            
            // 신청서 생성
            NewBannerApplication application = NewBannerApplication.builder()
                .applicantId(userId)
                .eventId(eventId)
                .bannerTypeId(getBannerTypeId(bannerType))
                .title(title)
                .imageUrl(imageUrl)
                .linkUrl(linkUrl)
                .startDate(minDate.atStartOfDay())
                .endDate(maxDate.atTime(23, 59, 59))
                .totalAmount((int) totalAmount)
                .applicationStatus(NewBannerApplication.ApplicationStatus.PENDING)
                .paymentStatus(NewBannerApplication.PaymentStatus.PENDING)
                .build();
            
            application = applicationRepository.save(application);
            
            // 로그 기록
            logActivity(application.getId(), "CREATED", "", "PENDING", userId, 
                "새 배너 신청서가 생성되었습니다.");
            
            log.info("배너 신청서 생성 완료 - ID: {}", application.getId());
            return application.getId();
            
        } catch (Exception e) {
            log.error("배너 신청서 생성 실패", e);
            throw new RuntimeException("배너 신청서 생성에 실패했습니다: " + e.getMessage());
        }
    }
    
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof String) {
            return Long.valueOf((String) value);
        }
        throw new IllegalArgumentException("Invalid value for key: " + key);
    }
    
    private int getPriceForHeroRank(int rank) {
        switch (rank) {
            case 1: return 2500000;
            case 2: return 2200000;
            case 3: return 2000000;
            case 4: return 1800000;
            case 5: return 1600000;
            case 6: return 1400000;
            case 7: return 1200000;
            case 8: return 1000000;
            case 9: return 800000;
            case 10: return 600000;
            default: return 600000;
        }
    }
    
    private Long getBannerTypeId(String typeCode) {
        return typeRepository.findByCode(typeCode)
            .map(NewBannerType::getId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid banner type: " + typeCode));
    }
}