package com.fairing.fairplay.payment.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.payment.dto.AdminRefundListRequestDto;
import com.fairing.fairplay.payment.dto.AdminRefundListResponseDto;
import com.fairing.fairplay.payment.dto.RefundApprovalDto;
import com.fairing.fairplay.payment.dto.RefundResponseDto;
import com.fairing.fairplay.payment.entity.Refund;
import com.fairing.fairplay.payment.entity.RefundStatusCode;
import com.fairing.fairplay.payment.repository.RefundRepository;
import com.fairing.fairplay.payment.repository.RefundStatusCodeRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AdminRefundService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EVENT_MANAGER = "EVENT_MANAGER";

    private final RefundRepository refundRepository;
    private final RefundStatusCodeRepository refundStatusCodeRepository;
    private final UserRepository userRepository;
    private final RefundService refundService;

    /**
     * 관리자용 환불 목록 조회 (필터링 및 페이징 지원)
     */
    @Transactional(readOnly = true)
    public Page<AdminRefundListResponseDto> getAdminRefundList(
            AdminRefundListRequestDto request, 
            CustomUserDetails userDetails) {
        
        // 권한 검증
        validateAdminAccess(userDetails);
        
        // 이벤트 관리자의 경우 자신이 관리하는 이벤트만 조회할 수 있도록 제한
        Long eventId = request.getEventId();
        Long managerUserId = getManagerUserIdForFiltering(userDetails);
        
        // 날짜 파싱
        LocalDateTime paymentDateFrom = parseDateTime(request.getPaymentDateFrom());
        LocalDateTime paymentDateTo = parseDateTime(request.getPaymentDateTo());
        
        // 정렬 및 페이징 설정
        Sort sort = Sort.by(
            "desc".equalsIgnoreCase(request.getSortDirection()) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC,
            request.getSortBy()
        );
        
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        
        // 쿼리 실행
        return refundRepository.findAdminRefundsWithFilters(
            request.getEventName(),
            request.getUserName(),
            paymentDateFrom,
            paymentDateTo,
            request.getRefundStatus(),
            request.getPaymentTargetType(),
            eventId,
            managerUserId,
            pageable
        );
    }

    /**
     * 환불 승인 처리
     */
    public RefundResponseDto approveRefund(Long refundId, RefundApprovalDto approval, CustomUserDetails userDetails) {
        refundService.approveRefund(refundId, approval, userDetails);
        refundService.recordRefundApprovalMetadata(refundId, approval, userDetails);
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("환불 요청을 찾을 수 없습니다: " + refundId));
        return RefundResponseDto.fromEntity(refund);
    }

    /**
     * 환불 거부 처리
     */
    @Transactional
    public RefundResponseDto rejectRefund(Long refundId, RefundApprovalDto rejection, CustomUserDetails userDetails) {
        
        validateAdminAccess(userDetails);
        
        // 환불 정보 조회
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("환불 요청을 찾을 수 없습니다: " + refundId));
        validateRefundAccess(refund, userDetails);
        
        // 현재 상태 검증
        if (!"REQUESTED".equals(refund.getRefundStatusCode().getCode())) {
            throw new IllegalStateException("거부 가능한 상태가 아닙니다. 현재 상태: " + refund.getRefundStatusCode().getName());
        }
        
        // 승인자 정보
        Users approver = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("승인자 정보를 찾을 수 없습니다."));
        
        // 환불 상태를 REJECTED로 변경
        RefundStatusCode rejectedStatus = refundStatusCodeRepository.findByCode("REJECTED")
                .orElseThrow(() -> new IllegalStateException("REJECTED 상태 코드를 찾을 수 없습니다."));
        
        refund.setRefundStatusCode(rejectedStatus);
        refund.setApprovedAt(LocalDateTime.now());
        refund.setApprovedBy(approver);
        refund.setAdminComment(rejection.getAdminComment());
        
        Refund savedRefund = refundRepository.save(refund);
        
        return RefundResponseDto.fromEntity(savedRefund);
    }

    /**
     * 관리자 권한 검증
     */
    private void validateAdminAccess(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        
        String roleCode = userDetails.getRoleCode();
        if (!ROLE_ADMIN.equals(roleCode) && !ROLE_EVENT_MANAGER.equals(roleCode)) {
            throw new AccessDeniedException("환불 관리 권한이 없습니다.");
        }
    }

    /**
     * 사용자 권한에 따른 관리자 필터링 값 결정
     */
    private Long getManagerUserIdForFiltering(CustomUserDetails userDetails) {
        if (ROLE_ADMIN.equals(userDetails.getRoleCode())) {
            return null;
        } else if (ROLE_EVENT_MANAGER.equals(userDetails.getRoleCode())) {
            return userDetails.getUserId();
        }
        throw new AccessDeniedException("환불 관리 권한이 없습니다.");
    }

    private void validateRefundAccess(Refund refund, CustomUserDetails userDetails) {
        if (ROLE_ADMIN.equals(userDetails.getRoleCode())) {
            return;
        }

        if (!ROLE_EVENT_MANAGER.equals(userDetails.getRoleCode())) {
            throw new AccessDeniedException("환불 관리 권한이 없습니다.");
        }

        if (refund.getPayment() == null || refund.getPayment().getEvent() == null) {
            throw new AccessDeniedException("행사에 연결되지 않은 환불은 전체 관리자만 처리할 수 있습니다.");
        }

        Long managerUserId = refund.getPayment().getEvent().getManager() != null
                ? refund.getPayment().getEvent().getManager().getUserId()
                : null;
        if (!userDetails.getUserId().equals(managerUserId)) {
            throw new AccessDeniedException("담당 행사의 환불만 처리할 수 있습니다.");
        }
    }

    /**
     * 날짜 문자열 파싱
     */
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
}
