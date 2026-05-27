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
import com.fairing.fairplay.user.entity.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefundService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EVENT_MANAGER = "EVENT_MANAGER";
    private static final int IAMPORT_CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int IAMPORT_READ_TIMEOUT_MILLIS = 10_000;

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentStatusCodeRepository paymentStatusCodeRepository;
    private final RefundStatusCodeRepository refundStatusCodeRepository;
    private final PlatformTransactionManager transactionManager;
    
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
    public PaymentResponseDto approveRefund(Long refundId, RefundApprovalDto approval, CustomUserDetails userDetails) {
        RefundApprovalSnapshot snapshot = reserveRefundApproval(refundId, approval, userDetails);

        try {
            // 아임포트에 토큰 요청
            String accessToken = getToken();
            try {
                // 아임포트 환불 요청 (금액 지정)
                processRefundRequest(
                        accessToken,
                        snapshot.merchantUid(),
                        snapshot.actualRefundAmount(),
                        snapshot.expectedCancelledAmount(),
                        snapshot.reason());
            } catch (IamportRefundRejectedException e) {
                markRefundProcessingFailed(snapshot.refundId(), e.getMessage());
                throw new RuntimeException("환불 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
            } catch (IOException e) {
                markRefundReconciliationRequired(snapshot.refundId(), e.getMessage());
                throw new RuntimeException("환불 처리 결과 확인이 필요합니다: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            markRefundProcessingFailed(snapshot.refundId(), e.getMessage());
            throw new RuntimeException("환불 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

        try {
            return completeReservedRefundApproval(snapshot.refundId(), snapshot.actualRefundAmount(), userDetails);
        } catch (RuntimeException e) {
            markRefundReconciliationRequired(snapshot.refundId(), e.getMessage());
            throw e;
        }
    }

    public void recordRefundApprovalMetadata(Long refundId, RefundApprovalDto approval, CustomUserDetails userDetails) {
        writeTransactionTemplate().executeWithoutResult(status -> {
            Refund refund = refundRepository.findByIdForUpdate(refundId)
                    .orElseThrow(() -> new IllegalArgumentException("환불 요청이 없습니다."));
            validateRefundAccess(refund, userDetails);
            Users approver = null;
            if (userDetails != null && userDetails.getUserId() != null) {
                approver = new Users(userDetails.getUserId());
            }
            refund.setApprovedBy(approver);
            if (approval != null) {
                refund.setAdminComment(approval.getAdminComment());
            }
        });
    }

    private static class IamportRefundRejectedException extends IOException {
        private IamportRefundRejectedException(String message) {
            super(message);
        }
    }

    private RefundApprovalSnapshot reserveRefundApproval(Long refundId, RefundApprovalDto approval, CustomUserDetails userDetails) {
        return writeTransactionTemplate().execute(status -> {
            validateRefundManagerRole(userDetails);
            Refund refund = refundRepository.findByIdForUpdate(refundId)
                    .orElseThrow(() -> new IllegalArgumentException("환불 요청이 없습니다."));
            validateRefundAccess(refund, userDetails);
            validateRequestedRefund(refund);

            Payment payment = lockPaymentOrUseExisting(refund.getPayment());
            BigDecimal availableAmount = payment.getAmount()
                    .subtract(payment.getRefundedAmount())
                    .subtract(reservedAmountFor(payment.getPaymentId()));
            BigDecimal actualRefundAmount = requestedApprovalAmount(refund, approval);
            if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("환불 가능한 금액이 없습니다.");
            }
            if (actualRefundAmount.compareTo(availableAmount) > 0) {
                throw new IllegalStateException("승인 환불 금액이 환불 가능 금액을 초과합니다.");
            }
            BigDecimal expectedCancelledAmount = payment.getRefundedAmount().add(actualRefundAmount);

            RefundStatusCode processingStatus = refundStatusCodeRepository.findByCode("PROCESSING")
                    .orElseThrow(() -> new IllegalStateException("환불 상태 코드를 찾을 수 없습니다: PROCESSING"));
            refund.setAmount(actualRefundAmount);
            refund.setRefundStatusCode(processingStatus);
            refund.setFailureReason(null);

            return new RefundApprovalSnapshot(
                    refund.getRefundId(),
                    payment.getMerchantUid(),
                    actualRefundAmount,
                    expectedCancelledAmount,
                    refund.getReason());
        });
    }

    private PaymentResponseDto completeReservedRefundApproval(Long refundId, BigDecimal actualRefundAmount, CustomUserDetails userDetails) {
        return writeTransactionTemplate().execute(status -> {
            validateRefundManagerRole(userDetails);
            Refund refund = refundRepository.findByIdForUpdate(refundId)
                    .orElseThrow(() -> new IllegalArgumentException("환불 요청이 없습니다."));
            validateRefundAccess(refund, userDetails);
            validateProcessingRefund(refund);

            RefundStatusCode approvedStatus = refundStatusCodeRepository.findByCode("APPROVED")
                    .orElseThrow(() -> new IllegalStateException("환불 상태 코드를 찾을 수 없습니다: APPROVED"));

            Payment payment = lockPaymentOrUseExisting(refund.getPayment());
            BigDecimal availableAmount = payment.getAmount().subtract(payment.getRefundedAmount());
            if (actualRefundAmount.compareTo(availableAmount) > 0) {
                throw new IllegalStateException("환불 가능 금액이 승인 중 변경되었습니다.");
            }

            refund.setAmount(actualRefundAmount);
            refund.setRefundStatusCode(approvedStatus);
            refund.setApprovedAt(LocalDateTime.now());
            updatePaymentAfterRefund(payment, actualRefundAmount);
            return PaymentResponseDto.fromEntity(payment);
        });
    }

    private void markRefundProcessingFailed(Long refundId, String failureReason) {
        writeTransactionTemplate().executeWithoutResult(status -> {
            Refund refund = refundRepository.findByIdForUpdate(refundId)
                    .orElseThrow(() -> new IllegalArgumentException("환불 요청이 없습니다."));
            if (!"PROCESSING".equals(refund.getRefundStatusCode().getCode())) {
                return;
            }
            RefundStatusCode failedStatus = refundStatusCodeRepository.findByCode("FAILED")
                    .orElseThrow(() -> new IllegalStateException("환불 상태 코드를 찾을 수 없습니다: FAILED"));
            refund.setRefundStatusCode(failedStatus);
            refund.setFailureReason(truncateFailureReason(failureReason));
            refund.setProcessedAt(LocalDateTime.now());
        });
    }

    private void markRefundReconciliationRequired(Long refundId, String failureReason) {
        writeTransactionTemplate().executeWithoutResult(status -> {
            Refund refund = refundRepository.findByIdForUpdate(refundId)
                    .orElseThrow(() -> new IllegalArgumentException("환불 요청이 없습니다."));
            if ("APPROVED".equals(refund.getRefundStatusCode().getCode())) {
                return;
            }
            RefundStatusCode reconciliationStatus = refundStatusCodeRepository.findByCode("RECONCILIATION_REQUIRED")
                    .orElseThrow(() -> new IllegalStateException("환불 상태 코드를 찾을 수 없습니다: RECONCILIATION_REQUIRED"));
            refund.setRefundStatusCode(reconciliationStatus);
            refund.setFailureReason(truncateFailureReason(failureReason));
            refund.setProcessedAt(LocalDateTime.now());
        });
    }

    private void validateRequestedRefund(Refund refund) {
        if (!"REQUESTED".equals(refund.getRefundStatusCode().getCode())) {
            throw new IllegalStateException("이미 처리된 환불 요청입니다.");
        }
    }

    private void validateProcessingRefund(Refund refund) {
        if (!"PROCESSING".equals(refund.getRefundStatusCode().getCode())) {
            throw new IllegalStateException("환불 처리 선점 상태가 아닙니다.");
        }
    }

    private Payment lockPaymentOrUseExisting(Payment payment) {
        if (payment == null || payment.getPaymentId() == null) {
            throw new IllegalArgumentException("환불 결제 정보가 없습니다.");
        }
        Optional<Payment> lockedPayment = paymentRepository.findByIdForUpdate(payment.getPaymentId());
        return lockedPayment.orElse(payment);
    }

    private BigDecimal requestedApprovalAmount(Refund refund, RefundApprovalDto approval) {
        BigDecimal requestedAmount = approval != null && approval.getRefundAmount() != null
                ? approval.getRefundAmount()
                : refund.getAmount();
        if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("승인 환불 금액은 0보다 커야 합니다.");
        }
        if (requestedAmount.compareTo(refund.getAmount()) > 0) {
            throw new IllegalArgumentException("승인 환불 금액은 요청 금액을 초과할 수 없습니다.");
        }
        return requestedAmount;
    }

    private BigDecimal reservedAmountFor(Long paymentId) {
        BigDecimal reservedAmount = refundRepository.sumReservedAmountByPaymentId(paymentId);
        return reservedAmount != null ? reservedAmount : BigDecimal.ZERO;
    }

    private String truncateFailureReason(String failureReason) {
        String message = failureReason != null ? failureReason : "알 수 없는 환불 처리 오류";
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private TransactionTemplate readOnlyTransactionTemplate() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(true);
        return template;
    }

    private TransactionTemplate writeTransactionTemplate() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private record RefundApprovalSnapshot(
            Long refundId,
            String merchantUid,
            BigDecimal actualRefundAmount,
            BigDecimal expectedCancelledAmount,
            String reason
    ) {
    }

    /**
     * 환불 요청 거절
     */
    @Transactional
    public PaymentResponseDto rejectRefund(Long refundId, String rejectReason, CustomUserDetails userDetails) {
        validateRefundManagerRole(userDetails);
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("환불 요청이 없습니다."));
        validateRefundAccess(refund, userDetails);

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
            PaymentStatusCode refundedStatus = paymentStatusCodeRepository.findByCode("REFUNDED")
                    .orElseThrow(() -> new IllegalStateException("결제 상태 코드를 찾을 수 없습니다: REFUNDED"));
            payment.setPaymentStatusCode(refundedStatus);
        } else {
            // 부분 환불
            PaymentStatusCode partialRefundStatusCode = paymentStatusCodeRepository.findByCode("PARTIAL_REFUNDED")
                    .orElseThrow(() -> new IllegalStateException("결제 상태 코드를 찾을 수 없습니다: PARTIAL_REFUNDED"));
            payment.setPaymentStatusCode(partialRefundStatusCode);
        }
    }

    /**
     * 환불 요청 (부분/전체 통합)
     */
    public static void refundRequest(String accessToken, String merchantUid, BigDecimal amount, String reason) throws IOException {
        refundRequest(accessToken, merchantUid, amount, amount, reason);
    }

    public static void refundRequest(
            String accessToken,
            String merchantUid,
            BigDecimal amount,
            BigDecimal expectedCancelledAmount,
            String reason
    ) throws IOException {
        URL url = new URL("https://api.iamport.kr/payments/cancel");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setConnectTimeout(IAMPORT_CONNECT_TIMEOUT_MILLIS);
        conn.setReadTimeout(IAMPORT_READ_TIMEOUT_MILLIS);

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

        try {
            int httpStatus = conn.getResponseCode();
            String response = readIamportResponse(conn, httpStatus);
            System.out.println("Refund response: " + response);
            if (httpStatus >= 400) {
                throw new IamportRefundRejectedException("아임포트 환불 HTTP 오류: " + httpStatus + ", body: " + response);
            }
            validateIamportCancelSuccess(response, merchantUid, amount, expectedCancelledAmount);
        } finally {
            conn.disconnect();
        }
    }

    protected void processRefundRequest(String accessToken, String merchantUid, BigDecimal amount, String reason) throws IOException {
        processRefundRequest(accessToken, merchantUid, amount, amount, reason);
    }

    protected void processRefundRequest(
            String accessToken,
            String merchantUid,
            BigDecimal amount,
            BigDecimal expectedCancelledAmount,
            String reason
    ) throws IOException {
        refundRequest(accessToken, merchantUid, amount, expectedCancelledAmount, reason);
    }

    /**
     * 모든 환불 요청 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public List<RefundResponseDto> getAllRefunds(Long eventId, CustomUserDetails userDetails) {
        validateRefundManagerRole(userDetails);

        List<Refund> refunds;
        if (ROLE_ADMIN.equals(userDetails.getRoleCode())) {
            if (eventId != null) {
                refunds = refundRepository.findByEventId(eventId);
            } else {
                refunds = refundRepository.findAll();
            }
        } else if (eventId != null) {
            refunds = refundRepository.findByEventId(eventId);
            refunds.forEach(refund -> validateRefundAccess(refund, userDetails));
        } else {
            refunds = refundRepository.findByEventManagerUserId(userDetails.getUserId());
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
        validateRefundManagerRole(userDetails);
        RefundStatusCode requestedStatus = refundStatusCodeRepository.findByCode("REQUESTED")
                .orElseThrow(() -> new IllegalStateException("환불 상태 코드를 찾을 수 없습니다: REQUESTED"));
        
        List<Refund> refunds;
        if (ROLE_ADMIN.equals(userDetails.getRoleCode())) {
            if (eventId != null) {
                refunds = refundRepository.findByEventIdAndRefundStatusCode(eventId, requestedStatus);
            } else {
                refunds = refundRepository.findByRefundStatusCode(requestedStatus);
            }
        } else if (eventId != null) {
            refunds = refundRepository.findByEventIdAndRefundStatusCode(eventId, requestedStatus);
            refunds.forEach(refund -> validateRefundAccess(refund, userDetails));
        } else {
            refunds = refundRepository.findByEventManagerUserIdAndRefundStatusCode(userDetails.getUserId(), requestedStatus);
        }

        return RefundResponseDto.fromEntityList(refunds);
    }

    /**
     * 아임포트 액세스 토큰 획득
     */
    public String getToken() throws IOException {

        URL url = new URL("https://api.iamport.kr/users/getToken");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setConnectTimeout(IAMPORT_CONNECT_TIMEOUT_MILLIS);
        conn.setReadTimeout(IAMPORT_READ_TIMEOUT_MILLIS);

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
            validateIamportSuccess(topLevelMap, "아임포트 토큰");
            Map<String, Object> responseMap = (Map<String, Object>) topLevelMap.get("response");
            if (responseMap == null || responseMap.get("access_token") == null) {
                throw new IOException("아임포트 토큰 응답에 access_token이 없습니다.");
            }
            accessToken = responseMap.get("access_token").toString();
        }

        conn.disconnect();
        return accessToken;
    }

    private static String readIamportResponse(HttpsURLConnection conn, int httpStatus) throws IOException {
        InputStream stream = httpStatus >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            return br.readLine();
        }
    }

    private static void validateIamportSuccess(String jsonLine, String operation) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> topLevelMap = objectMapper.readValue(jsonLine, Map.class);
        validateIamportSuccess(topLevelMap, operation);
    }

    static void validateIamportCancelSuccess(
            String jsonLine,
            String merchantUid,
            BigDecimal amount,
            BigDecimal expectedCancelledAmount
    ) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> topLevelMap = objectMapper.readValue(jsonLine, Map.class);
        validateIamportSuccess(topLevelMap, "아임포트 환불");

        Object response = topLevelMap.get("response");
        if (!(response instanceof Map<?, ?> responseMap)) {
            throw new IamportRefundRejectedException("아임포트 환불 응답에 response가 없습니다.");
        }
        Object responseMerchantUid = responseMap.get("merchant_uid");
        if (!merchantUid.equals(String.valueOf(responseMerchantUid))) {
            throw new IamportRefundRejectedException("아임포트 환불 주문번호가 일치하지 않습니다.");
        }
        BigDecimal totalCancelledAmount = amountFrom(responseMap.get("cancel_amount"));
        BigDecimal currentCancelledAmount = latestCancelHistoryAmount(responseMap);
        boolean totalMatches = totalCancelledAmount != null
                && totalCancelledAmount.compareTo(expectedCancelledAmount) == 0;
        boolean currentMatches = currentCancelledAmount != null
                && currentCancelledAmount.compareTo(amount) == 0;
        if (!totalMatches && !currentMatches) {
            throw new IamportRefundRejectedException("아임포트 환불 금액이 일치하지 않습니다.");
        }
    }

    private static BigDecimal latestCancelHistoryAmount(Map<?, ?> responseMap) throws IamportRefundRejectedException {
        Object cancelHistory = responseMap.get("cancel_history");
        if (!(cancelHistory instanceof List<?> history) || history.isEmpty()) {
            return null;
        }
        Object latestCancel = history.get(history.size() - 1);
        if (!(latestCancel instanceof Map<?, ?> latestCancelMap)) {
            return null;
        }
        return amountFrom(latestCancelMap.get("amount"));
    }

    private static BigDecimal amountFrom(Object amount) throws IamportRefundRejectedException {
        try {
            if (amount instanceof Number number) {
                return new BigDecimal(number.toString());
            }
            if (amount != null) {
                return new BigDecimal(String.valueOf(amount));
            }
            return null;
        } catch (NumberFormatException e) {
            throw new IamportRefundRejectedException("아임포트 환불 금액 형식이 올바르지 않습니다.");
        }
    }

    private static void validateIamportSuccess(Map<String, Object> topLevelMap, String operation) throws IOException {
        Object code = topLevelMap.get("code");
        boolean success = code instanceof Number number
                ? number.intValue() == 0
                : "0".equals(String.valueOf(code));
        if (!success) {
            throw new IamportRefundRejectedException(operation + " 실패: " + topLevelMap.get("message"));
        }
    }

    /**
     * 환불 목록 조회 (필터링 및 페이징 지원)
     */
    @Transactional(readOnly = true)
    public Page<RefundListResponseDto> getRefundList(RefundListRequestDto request, CustomUserDetails userDetails) {
        validateRefundManagerRole(userDetails);
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
            Long managerUserId = ROLE_EVENT_MANAGER.equals(userDetails.getRoleCode()) ? userDetails.getUserId() : null;
            return refundRepository.findRefundsWithFilters(
                request.getEventName(),
                paymentDateFrom,
                paymentDateTo,
                request.getRefundStatus(),
                request.getPaymentTargetType(),
                managerUserId,
                pageable
            );
            
        } catch (Exception e) {
            throw new RuntimeException("환불 목록 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private void validateRefundManagerRole(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        String roleCode = userDetails.getRoleCode();
        if (!ROLE_ADMIN.equals(roleCode) && !ROLE_EVENT_MANAGER.equals(roleCode)) {
            throw new AccessDeniedException("환불 관리 권한이 없습니다.");
        }
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
