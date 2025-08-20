package com.fairing.fairplay.payment.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.payment.dto.*;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.PaymentStatusCode;
import com.fairing.fairplay.payment.entity.Refund;
import com.fairing.fairplay.payment.entity.RefundStatusCode;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.payment.repository.PaymentStatusCodeRepository;
import com.fairing.fairplay.payment.repository.RefundRepository;
import com.fairing.fairplay.payment.repository.RefundStatusCodeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentStatusCodeRepository paymentStatusCodeRepository;
    private final RefundStatusCodeRepository refundStatusCodeRepository;
    
    @Value("${iamport.api-key}")
    private String iamportApiKey;
    
    @Value("${iamport.secret-key}")
    private String iamportSecretKey;

    /**
     * 1단계: 구매자의 환불 요청 - DB에만 저장, 아임포트 호출하지 않음
     */
    @Transactional
    public PaymentResponseDto requestRefund(Long paymentId, PaymentRequestDto paymentRequestDto, Long userId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역이 없습니다."));

        // 본인의 결제인지 확인
        if (!payment.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 결제 내역만 환불 요청할 수 있습니다.");
        }

        // 환불 가능한 상태인지 확인
        validateRefundRequest(payment, paymentRequestDto.getRefundRequestAmount());

        // 환불 요청 상태 코드 조회 (REQUESTED)
        RefundStatusCode requestedStatus = refundStatusCodeRepository.findByCode("REQUESTED")
                .orElseThrow(() -> new IllegalStateException("환불 상태 코드를 찾을 수 없습니다: REQUESTED"));

        // 환불 요청 정보를 DB에 저장 (상태: REQUESTED)
        Refund refund = Refund.builder()
                .payment(payment)
                .amount(paymentRequestDto.getRefundRequestAmount())
                .reason(paymentRequestDto.getReason())
                .refundStatusCode(requestedStatus)  // 요청 상태
                .createdAt(LocalDateTime.now())
                .build();
        refundRepository.save(refund);

        return PaymentResponseDto.fromEntity(payment);
    }

    /**
     * 2단계: 관리자의 환불 승인 - 이때 아임포트에 환불 요청
     */
    @Transactional
    public PaymentResponseDto approveRefund(Long refundId, RefundApprovalDto approval, Long adminUserId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("환불 요청이 없습니다."));

        // 이미 처리된 요청인지 확인
        if (!"REQUESTED".equals(refund.getRefundStatusCode().getCode())) {
            throw new IllegalStateException("이미 처리된 환불 요청입니다.");
        }

        Payment payment = refund.getPayment();

        try {
            // 아임포트에 토큰 요청
            String accessToken = getToken();

            RefundStatusCode approvedStatus = refundStatusCodeRepository.findByCode("APPROVED")
                    .orElseThrow(() -> new IllegalStateException("환불 상태 코드를 찾을 수 없습니다: APPROVED"));

            // 환불 가능한 최대 금액 계산
            BigDecimal totalRefundedAmount = payment.getRefundedAmount();
            BigDecimal availableAmount = payment.getAmount().subtract(totalRefundedAmount);
            
            // 요청 금액이 남은 금액보다 크면 남은 금액으로 조정
            BigDecimal actualRefundAmount = refund.getAmount().min(availableAmount);
            
            // 아임포트 환불 요청 (금액 지정)
            refundRequest(accessToken, payment.getMerchantUid(), actualRefundAmount, refund.getReason());
            
            // 실제 환불 금액으로 조정
            refund.setAmount(actualRefundAmount);

            // 환불 승인 처리
            refund.setRefundStatusCode(approvedStatus);
            refund.setApprovedAt(LocalDateTime.now());

            // 결제 정보 업데이트
            updatePaymentAfterRefund(payment, refund.getAmount());

        } catch (IOException e) {
            // 환불 거부 상태 코드 조회 (REJECTED)
            RefundStatusCode rejectedStatus = refundStatusCodeRepository.findByCode("REJECTED")
                    .orElseThrow(() -> new IllegalStateException("환불 상태 코드를 찾을 수 없습니다: REJECTED"));
            
            // 아임포트 환불 실패 시 상태를 REJECTED로 변경
            refund.setRefundStatusCode(rejectedStatus);
            throw new RuntimeException("환불 처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        return PaymentResponseDto.fromEntity(payment);
    }

    /**
     * 환불 요청 거절
     */
    @Transactional
    public PaymentResponseDto rejectRefund(Long refundId, String rejectReason, Long adminUserId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("환불 요청이 없습니다."));

        if (!"REQUESTED".equals(refund.getRefundStatusCode().getCode())) {
            throw new IllegalStateException("이미 처리된 환불 요청입니다.");
        }

        // 환불 거부 상태 코드 조회 (REJECTED)
        RefundStatusCode rejectedStatus = refundStatusCodeRepository.findByCode("REJECTED")
                .orElseThrow(() -> new IllegalStateException("환불 상태 코드를 찾을 수 없습니다: REJECTED"));

        refund.setRefundStatusCode(rejectedStatus);
        refund.setApprovedAt(LocalDateTime.now()); // 처리일시
        // 거절 사유는 별도 필드가 필요하면 추가

        return PaymentResponseDto.fromEntity(refund.getPayment());
    }

    /**
     * 환불 요청 유효성 검증
     */
    private void validateRefundRequest(Payment payment, BigDecimal refundAmount) {
        // 환불 불가능한 상태 검증 (취소됨, 실패 등은 제외)
        Integer statusCodeId = payment.getPaymentStatusCode().getPaymentStatusCodeId();
        if (statusCodeId == 1 || statusCodeId == 3 || statusCodeId == 4) { // PENDING, FAILED, REFUNDED
            throw new IllegalStateException("환불할 수 없는 결제 상태입니다. 상태코드: " + statusCodeId);
        }

        // 환불 가능한 금액 계산 (DB 컬럼 활용)
        BigDecimal availableRefundAmount = payment.getAmount().subtract(payment.getRefundedAmount());

        if (refundAmount.compareTo(availableRefundAmount) > 0) {
            throw new IllegalArgumentException("환불 가능 금액을 초과했습니다. 환불 가능 금액: " + availableRefundAmount);
        }

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }
    }

    /**
     * 환불 후 결제 정보 업데이트
     */
    private void updatePaymentAfterRefund(Payment payment, BigDecimal refundAmount) {
        // 환불 금액 누적 업데이트
        BigDecimal currentRefundedAmount = payment.getRefundedAmount();
        payment.setRefundedAmount(currentRefundedAmount.add(refundAmount));

        // 환불 시점 업데이트 (부분/전체 환불 모두)
        payment.setRefundedAt(LocalDateTime.now());
        
        // 결제 상태 업데이트 (금액 기준으로 판단)
        if (payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0) {
            // 전체 환불
            PaymentStatusCode refundedStatus = paymentStatusCodeRepository.getReferenceById(4); // REFUNDED
            payment.setPaymentStatusCode(refundedStatus);
        } else {
            // 부분 환불
            PaymentStatusCode partialRefundStatusCode = paymentStatusCodeRepository.getReferenceById(5); // PARTIAL_REFUNDED
            payment.setPaymentStatusCode(partialRefundStatusCode);
        }
    }

    /**
     * 환불 요청 (부분/전체 통합)
     */
    public static void refundRequest(String accessToken, String merchantUid, BigDecimal amount, String reason) throws IOException {
        URL url = new URL("https://api.iamport.kr/payments/cancel");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", accessToken);
        conn.setDoOutput(true);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("merchant_uid", merchantUid);
        objectNode.put("amount", amount);  // 환불 금액
        objectNode.put("reason", reason);

        String json = mapper.writeValueAsString(objectNode);

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
            bw.write(json);
            bw.flush();
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String response = br.readLine();
            System.out.println("Refund response: " + response);
            // 필요시 응답 파싱하여 결과 확인
        }

        conn.disconnect();
    }

    /**
     * 모든 환불 요청 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public List<RefundResponseDto> getAllRefunds(Long eventId, CustomUserDetails userDetails) {
        // 임시 하드코딩: ADMIN 권한으로 가정
        String roleCode = "ADMIN";
        // if(userDetails != null) {
        //     roleCode = userDetails.getRoleCode();
        //     if (!"ADMIN".equals(roleCode) && !"EVENT_MANAGER".equals(roleCode)) {
        //         throw new IllegalArgumentException("권한이 없습니다.");
        //     }
        // }

        List<Refund> refunds;
        if (eventId != null) {
            refunds = refundRepository.findByEventId(eventId);
        } else {
            refunds = refundRepository.findAll();
        }

        return RefundResponseDto.fromEntityList(refunds);
    }

    /**
     * 내 환불 요청 조회 (구매자용)
     */
    @Transactional(readOnly = true)
    public List<RefundResponseDto> getMyRefunds(Long userId) {
        List<Refund> refunds = refundRepository.findByUserId(userId);
        return RefundResponseDto.fromEntityList(refunds);
    }

    /**
     * 내 환불 목록 조회 (RefundListResponseDto 형태)
     */
    @Transactional(readOnly = true)
    public List<RefundListResponseDto> getMyRefundList(Long userId) {
        try {
            List<Refund> refunds = refundRepository.findByUserId(userId);
            return refunds.stream()
                    .map(this::convertToRefundListResponseDto)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("내 환불 목록 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * Refund 엔티티를 RefundListResponseDto로 변환
     */
    private RefundListResponseDto convertToRefundListResponseDto(Refund refund) {
        return RefundListResponseDto.builder()
                .refundId(refund.getRefundId())
                .paymentId(refund.getPayment().getPaymentId())
                .merchantUid(refund.getPayment().getMerchantUid())
                .eventId(refund.getPayment().getEvent() != null ? refund.getPayment().getEvent().getEventId() : null)
                .eventName(refund.getPayment().getEvent() != null ? refund.getPayment().getEvent().getTitleKr() : null)
                .userId(refund.getPayment().getUser().getUserId())
                .userName(refund.getPayment().getUser().getName())
                .userEmail(refund.getPayment().getUser().getEmail())
                .userPhone(refund.getPayment().getUser().getPhone())
                .paymentTargetType(refund.getPayment().getPaymentTargetType().getPaymentTargetCode())
                .paymentTargetName(refund.getPayment().getPaymentTargetType().getPaymentTargetName())
                .targetId(refund.getPayment().getTargetId())
                .quantity(refund.getPayment().getQuantity())
                .price(refund.getPayment().getPrice())
                .totalAmount(refund.getPayment().getAmount())
                .paidAt(refund.getPayment().getPaidAt())
                .refundAmount(refund.getAmount())
                .refundReason(refund.getReason())
                .refundStatus(refund.getRefundStatusCode().getCode())
                .refundStatusName(refund.getRefundStatusCode().getName())
                .refundCreatedAt(refund.getCreatedAt())
                .refundApprovedAt(refund.getApprovedAt())
                .build();
    }

    /**
     * 대기 중인 환불 요청 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public List<RefundResponseDto> getPendingRefunds(Long eventId, CustomUserDetails userDetails) {
        RefundStatusCode requestedStatus = refundStatusCodeRepository.findByCode("REQUESTED")
                .orElseThrow(() -> new IllegalStateException("환불 상태 코드를 찾을 수 없습니다: REQUESTED"));
        
        List<Refund> refunds;
        if (eventId != null) {
            refunds = refundRepository.findByEventIdAndRefundStatusCode(eventId, requestedStatus);
        } else {
            refunds = refundRepository.findByRefundStatusCode(requestedStatus);
        }

        return RefundResponseDto.fromEntityList(refunds);
    }

    /**
     * 아임포트 액세스 토큰 획득
     */
    public String getToken() throws IOException {

        URL url = new URL("https://api.iamport.kr/users/getToken");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("imp_key", iamportApiKey);
        objectNode.put("imp_secret", iamportSecretKey);

        String json = mapper.writeValueAsString(objectNode);

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
            bw.write(json);
            bw.flush();
        }

        String accessToken;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String jsonLine = br.readLine();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> topLevelMap = objectMapper.readValue(jsonLine, Map.class);
            Map<String, Object> responseMap = (Map<String, Object>) topLevelMap.get("response");
            accessToken = responseMap.get("access_token").toString();
        }

        conn.disconnect();
        return accessToken;
    }

    /**
     * 환불 목록 조회 (필터링 및 페이징 지원)
     */
    @Transactional(readOnly = true)
    public Page<RefundListResponseDto> getRefundList(RefundListRequestDto request) {
        try {
            // 날짜 문자열을 LocalDateTime으로 변환
            LocalDateTime paymentDateFrom = null;
            LocalDateTime paymentDateTo = null;
            
            if (request.getPaymentDateFrom() != null && !request.getPaymentDateFrom().trim().isEmpty()) {
                LocalDate fromDate = LocalDate.parse(request.getPaymentDateFrom(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                paymentDateFrom = fromDate.atStartOfDay();
            }
            
            if (request.getPaymentDateTo() != null && !request.getPaymentDateTo().trim().isEmpty()) {
                LocalDate toDate = LocalDate.parse(request.getPaymentDateTo(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                paymentDateTo = toDate.atTime(LocalTime.MAX);
            }

            // 페이징 정보 생성
            Sort sort = Sort.by(
                "desc".equalsIgnoreCase(request.getSortDirection()) 
                    ? Sort.Direction.DESC 
                    : Sort.Direction.ASC,
                request.getSortBy()
            );
            
            Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

            // Repository 메서드 호출
            return refundRepository.findRefundsWithFilters(
                request.getEventName(),
                paymentDateFrom,
                paymentDateTo,
                request.getRefundStatus(),
                request.getPaymentTargetType(),
                pageable
            );
            
        } catch (Exception e) {
            throw new RuntimeException("환불 목록 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 환불 요청 (새로운 DTO 사용)
     */
    @Transactional
    public PaymentResponseDto requestRefundWithDto(RefundRequestDto refundRequest, Long userId) {
        Payment payment = paymentRepository.findById(refundRequest.getPaymentId())
                .orElseThrow(() -> new IllegalArgumentException("결제 내역이 없습니다."));

        // 본인의 결제인지 확인
        if (!payment.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 결제 내역만 환불 요청할 수 있습니다.");
        }

        BigDecimal refundAmount;
        
        if ("FULL".equals(refundRequest.getRefundType())) {
            // 전체 환불
            refundAmount = payment.getAmount().subtract(payment.getRefundedAmount());
        } else {
            // 부분 환불
            if (refundRequest.getRefundQuantity() != null && refundRequest.getRefundQuantity() > 0) {
                // 수량 기반 환불
                BigDecimal unitPrice = payment.getPrice();
                refundAmount = unitPrice.multiply(BigDecimal.valueOf(refundRequest.getRefundQuantity()));
            } else if (refundRequest.getRefundAmount() != null) {
                // 금액 기반 환불
                refundAmount = refundRequest.getRefundAmount();
            } else {
                throw new IllegalArgumentException("부분 환불의 경우 환불 금액 또는 수량을 지정해야 합니다.");
            }
        }

        // 환불 가능한 상태인지 확인
        validateRefundRequest(payment, refundAmount);

        // 환불 요청 상태 코드 조회 (REQUESTED)
        RefundStatusCode requestedStatus = refundStatusCodeRepository.findByCode("REQUESTED")
                .orElseThrow(() -> new IllegalStateException("환불 상태 코드를 찾을 수 없습니다: REQUESTED"));

        // 환불 요청 정보를 DB에 저장 (상태: REQUESTED)
        Refund refund = Refund.builder()
                .payment(payment)
                .amount(refundAmount)
                .reason(refundRequest.getReason())
                .refundStatusCode(requestedStatus)
                .createdAt(LocalDateTime.now())
                .build();
        refundRepository.save(refund);

        return PaymentResponseDto.fromEntity(payment);
    }
}