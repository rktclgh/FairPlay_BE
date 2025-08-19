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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AdminRefundService {

    private final RefundRepository refundRepository;
    private final RefundStatusCodeRepository refundStatusCodeRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService; // PG사 환불 처리를 위해

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
        Long eventId = getEventIdForUser(request.getEventId(), userDetails);
        
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
            pageable
        );
    }

    /**
     * 환불 승인 처리
     */
    @Transactional
    public RefundResponseDto approveRefund(Long refundId, RefundApprovalDto approval, CustomUserDetails userDetails) {
        
        validateAdminAccess(userDetails);
        
        // 환불 정보 조회
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("환불 요청을 찾을 수 없습니다: " + refundId));
        
        // 현재 상태 검증
        if (!"REQUESTED".equals(refund.getRefundStatusCode().getCode())) {
            throw new IllegalStateException("승인 가능한 상태가 아닙니다. 현재 상태: " + refund.getRefundStatusCode().getName());
        }
        
        // 승인자 정보
        Users approver = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("승인자 정보를 찾을 수 없습니다."));
        
        // 환불 상태를 APPROVED로 변경
        RefundStatusCode approvedStatus = refundStatusCodeRepository.findByCode("APPROVED")
                .orElseThrow(() -> new IllegalStateException("APPROVED 상태 코드를 찾을 수 없습니다."));
        
        refund.setRefundStatusCode(approvedStatus);
        refund.setApprovedAt(LocalDateTime.now());
        refund.setApprovedBy(approver);
        refund.setAdminComment(approval.getAdminComment());
        
        // 환불 금액 수정이 있는 경우
        if (approval.getRefundAmount() != null && 
            approval.getRefundAmount().compareTo(refund.getAmount()) != 0) {
            refund.setAmount(approval.getRefundAmount());
        }
        
        Refund savedRefund = refundRepository.save(refund);
        
        // 즉시 PG사 환불 처리 요청인 경우
        if (Boolean.TRUE.equals(approval.getProcessImmediately())) {
            try {
                processIamportRefund(savedRefund);
            } catch (Exception e) {
                // PG사 환불 실패 시 상태를 FAILED로 변경하고 에러 로그
                handleRefundFailure(savedRefund, "PG사 환불 처리 실패: " + e.getMessage());
                throw new RuntimeException("환불 승인은 완료되었으나 PG사 환불 처리에 실패했습니다: " + e.getMessage());
            }
        }
        
        return RefundResponseDto.fromEntity(savedRefund);
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
     * PG사 환불 처리 (아임포트)
     */
    private void processIamportRefund(Refund refund) {
        // 환불 상태를 PROCESSING으로 변경
        RefundStatusCode processingStatus = refundStatusCodeRepository.findByCode("PROCESSING")
                .orElseThrow(() -> new IllegalStateException("PROCESSING 상태 코드를 찾을 수 없습니다."));
        
        refund.setRefundStatusCode(processingStatus);
        refundRepository.save(refund);
        
        // TODO: PaymentService의 PG사 환불 메서드 호출
        // paymentService.processIamportRefund(refund.getPayment().getImpUid(), refund.getAmount(), refund.getReason());
        
        // 환불 성공 시 상태를 COMPLETED로 변경
        RefundStatusCode completedStatus = refundStatusCodeRepository.findByCode("COMPLETED")
                .orElseThrow(() -> new IllegalStateException("COMPLETED 상태 코드를 찾을 수 없습니다."));
        
        refund.setRefundStatusCode(completedStatus);
        refund.setProcessedAt(LocalDateTime.now());
        refundRepository.save(refund);
    }

    /**
     * 환불 실패 처리
     */
    private void handleRefundFailure(Refund refund, String failureReason) {
        RefundStatusCode failedStatus = refundStatusCodeRepository.findByCode("FAILED")
                .orElseThrow(() -> new IllegalStateException("FAILED 상태 코드를 찾을 수 없습니다."));
        
        refund.setRefundStatusCode(failedStatus);
        refund.setFailureReason(failureReason);
        refundRepository.save(refund);
    }

    /**
     * 관리자 권한 검증
     */
    private void validateAdminAccess(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        
        String roleCode = userDetails.getRoleCode();
        if (!"ADMIN".equals(roleCode) && !"EVENT_MANAGER".equals(roleCode)) {
            throw new IllegalArgumentException("환불 관리 권한이 없습니다.");
        }
    }

    /**
     * 사용자 권한에 따른 이벤트 ID 제한
     */
    private Long getEventIdForUser(Long requestedEventId, CustomUserDetails userDetails) {
        if ("ADMIN".equals(userDetails.getRoleCode())) {
            // 전체 관리자는 모든 이벤트 조회 가능
            return requestedEventId;
        } else if ("EVENT_MANAGER".equals(userDetails.getRoleCode())) {
            // 이벤트 관리자는 특정 이벤트만 조회 가능
            // TODO: 사용자가 관리하는 이벤트 ID 목록을 가져와서 검증
            return requestedEventId;
        }
        return requestedEventId;
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