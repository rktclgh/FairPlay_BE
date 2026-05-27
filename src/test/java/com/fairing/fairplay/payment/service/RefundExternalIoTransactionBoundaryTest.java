package com.fairing.fairplay.payment.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.payment.dto.RefundApprovalDto;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.PaymentStatusCode;
import com.fairing.fairplay.payment.entity.PaymentTargetType;
import com.fairing.fairplay.payment.entity.PaymentTypeCode;
import com.fairing.fairplay.payment.entity.Refund;
import com.fairing.fairplay.payment.entity.RefundStatusCode;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.payment.repository.PaymentStatusCodeRepository;
import com.fairing.fairplay.payment.repository.RefundRepository;
import com.fairing.fairplay.payment.repository.RefundStatusCodeRepository;
import com.fairing.fairplay.support.RecordingTransactionManager;
import com.fairing.fairplay.user.entity.Users;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefundExternalIoTransactionBoundaryTest {

    @Test
    void approveRefundCallsIamportOutsideDatabaseTransactions() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        RefundRepository refundRepository = mock(RefundRepository.class);
        PaymentStatusCodeRepository paymentStatusCodeRepository = mock(PaymentStatusCodeRepository.class);
        RefundStatusCodeRepository refundStatusCodeRepository = mock(RefundStatusCodeRepository.class);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        Refund refund = refund();
        TestRefundService refundService = new TestRefundService(
                paymentRepository,
                refundRepository,
                paymentStatusCodeRepository,
                refundStatusCodeRepository,
                transactionManager);

        when(refundRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(refund));
        when(paymentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(refund.getPayment()));
        when(refundRepository.sumReservedAmountByPaymentId(10L)).thenReturn(BigDecimal.ZERO);
        when(refundStatusCodeRepository.findByCode("PROCESSING")).thenReturn(Optional.of(status("PROCESSING")));
        when(refundStatusCodeRepository.findByCode("APPROVED")).thenReturn(Optional.of(status("APPROVED")));
        when(paymentStatusCodeRepository.findByCode("PARTIAL_REFUNDED")).thenReturn(Optional.of(paymentStatus(5)));

        refundService.approveRefund(1L, RefundApprovalDto.builder().build(), user(1L));

        assertThat(refundService.tokenRequestedOutsideTransaction).isTrue();
        assertThat(refundService.refundRequestedOutsideTransaction).isTrue();
        assertThat(refundService.refundRequestCount).isEqualTo(1);
        assertThat(refundService.requestedAmount).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(refundService.expectedCancelledAmount).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(transactionManager.activeCount()).isZero();
    }

    @Test
    void approveRefundUsesManagerAdjustedRefundAmountForIamportRequest() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        RefundRepository refundRepository = mock(RefundRepository.class);
        PaymentStatusCodeRepository paymentStatusCodeRepository = mock(PaymentStatusCodeRepository.class);
        RefundStatusCodeRepository refundStatusCodeRepository = mock(RefundStatusCodeRepository.class);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        Refund refund = refund();
        TestRefundService refundService = new TestRefundService(
                paymentRepository,
                refundRepository,
                paymentStatusCodeRepository,
                refundStatusCodeRepository,
                transactionManager);
        refund.getPayment().setRefundedAmount(BigDecimal.valueOf(20));

        when(refundRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(refund));
        when(paymentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(refund.getPayment()));
        when(refundRepository.sumReservedAmountByPaymentId(10L)).thenReturn(BigDecimal.ZERO);
        when(refundStatusCodeRepository.findByCode("PROCESSING")).thenReturn(Optional.of(status("PROCESSING")));
        when(refundStatusCodeRepository.findByCode("APPROVED")).thenReturn(Optional.of(status("APPROVED")));
        when(paymentStatusCodeRepository.findByCode("PARTIAL_REFUNDED")).thenReturn(Optional.of(paymentStatus(5)));

        refundService.approveRefund(1L, RefundApprovalDto.builder()
                .refundAmount(BigDecimal.valueOf(5))
                .build(), user(1L));

        assertThat(refundService.requestedAmount).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(refundService.expectedCancelledAmount).isEqualByComparingTo(BigDecimal.valueOf(25));
        assertThat(refund.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void approveRefundDoesNotCallIamportWhenRefundIsAlreadyReserved() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        RefundRepository refundRepository = mock(RefundRepository.class);
        PaymentStatusCodeRepository paymentStatusCodeRepository = mock(PaymentStatusCodeRepository.class);
        RefundStatusCodeRepository refundStatusCodeRepository = mock(RefundStatusCodeRepository.class);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        Refund refund = refund();
        refund.setRefundStatusCode(status("PROCESSING"));
        TestRefundService refundService = new TestRefundService(
                paymentRepository,
                refundRepository,
                paymentStatusCodeRepository,
                refundStatusCodeRepository,
                transactionManager);

        when(refundRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(refund));

        assertThatThrownBy(() -> refundService.approveRefund(1L, RefundApprovalDto.builder().build(), user(1L)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(refundService.refundRequestCount).isZero();
        assertThat(transactionManager.activeCount()).isZero();
    }

    @Test
    void approveRefundDoesNotCallIamportWhenProcessingRefundsReserveTheAvailableAmount() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        RefundRepository refundRepository = mock(RefundRepository.class);
        PaymentStatusCodeRepository paymentStatusCodeRepository = mock(PaymentStatusCodeRepository.class);
        RefundStatusCodeRepository refundStatusCodeRepository = mock(RefundStatusCodeRepository.class);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        Refund refund = refund();
        TestRefundService refundService = new TestRefundService(
                paymentRepository,
                refundRepository,
                paymentStatusCodeRepository,
                refundStatusCodeRepository,
                transactionManager);

        when(refundRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(refund));
        when(paymentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(refund.getPayment()));
        when(refundRepository.sumReservedAmountByPaymentId(10L)).thenReturn(BigDecimal.valueOf(100));

        assertThatThrownBy(() -> refundService.approveRefund(1L, RefundApprovalDto.builder().build(), user(1L)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(refundService.refundRequestCount).isZero();
        assertThat(refund.getRefundStatusCode().getCode()).isEqualTo("REQUESTED");
        assertThat(transactionManager.activeCount()).isZero();
    }

    @Test
    void approveRefundDoesNotSilentlyReduceRequestedAmountWhenOnlyPartialCapacityRemains() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        RefundRepository refundRepository = mock(RefundRepository.class);
        PaymentStatusCodeRepository paymentStatusCodeRepository = mock(PaymentStatusCodeRepository.class);
        RefundStatusCodeRepository refundStatusCodeRepository = mock(RefundStatusCodeRepository.class);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        Refund refund = refund();
        refund.setAmount(BigDecimal.valueOf(50));
        TestRefundService refundService = new TestRefundService(
                paymentRepository,
                refundRepository,
                paymentStatusCodeRepository,
                refundStatusCodeRepository,
                transactionManager);

        when(refundRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(refund));
        when(paymentRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(refund.getPayment()));
        when(refundRepository.sumReservedAmountByPaymentId(10L)).thenReturn(BigDecimal.valueOf(60));

        assertThatThrownBy(() -> refundService.approveRefund(1L, RefundApprovalDto.builder().build(), user(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("초과");

        assertThat(refundService.refundRequestCount).isZero();
        assertThat(refund.getRefundStatusCode().getCode()).isEqualTo("REQUESTED");
        assertThat(transactionManager.activeCount()).isZero();
    }

    @Test
    void iamportCancelValidationAcceptsCumulativeCancelAmountForPartialRefunds() {
        String response = """
                {
                  "code": 0,
                  "message": null,
                  "response": {
                    "merchant_uid": "merchant-10",
                    "amount": 100,
                    "cancel_amount": 30,
                    "cancel_history": [
                      {"amount": 10},
                      {"amount": 20}
                    ]
                  }
                }
                """;

        assertThatCode(() -> RefundService.validateIamportCancelSuccess(
                response,
                "merchant-10",
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(30)))
                .doesNotThrowAnyException();
    }

    @Test
    void iamportCancelValidationDoesNotTreatOriginalPaymentAmountAsCurrentRefundAmount() {
        String response = """
                {
                  "code": 0,
                  "message": null,
                  "response": {
                    "merchant_uid": "merchant-10",
                    "amount": 100,
                    "cancel_history": [
                      {"amount": 10}
                    ]
                  }
                }
                """;

        assertThatThrownBy(() -> RefundService.validateIamportCancelSuccess(
                response,
                "merchant-10",
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(30)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("금액");
    }

    private Refund refund() {
        Payment payment = Payment.builder()
                .paymentId(10L)
                .user(new Users(300L))
                .paymentTargetType(PaymentTargetType.builder()
                        .paymentTargetCode("RESERVATION")
                        .paymentTargetName("예약")
                        .build())
                .targetId(20L)
                .merchantUid("merchant-10")
                .impUid("imp-10")
                .quantity(1)
                .price(BigDecimal.valueOf(100))
                .amount(BigDecimal.valueOf(100))
                .refundedAmount(BigDecimal.ZERO)
                .paymentTypeCode(PaymentTypeCode.builder().paymentTypeCodeId(1).code("CARD").name("카드").build())
                .paymentStatusCode(paymentStatus(2))
                .build();

        return Refund.builder()
                .refundId(1L)
                .payment(payment)
                .amount(BigDecimal.TEN)
                .reason("환불 사유")
                .refundStatusCode(status("REQUESTED"))
                .build();
    }

    private RefundStatusCode status(String code) {
        return RefundStatusCode.builder()
                .code(code)
                .name(code)
                .build();
    }

    private PaymentStatusCode paymentStatus(Integer id) {
        return PaymentStatusCode.builder()
                .paymentStatusCodeId(id)
                .code("STATUS")
                .name("상태")
                .build();
    }

    private CustomUserDetails user(Long userId) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getRoleCode()).thenReturn("ADMIN");
        return userDetails;
    }

    private static class TestRefundService extends RefundService {

        private final RecordingTransactionManager transactionManager;
        private boolean tokenRequestedOutsideTransaction;
        private boolean refundRequestedOutsideTransaction;
        private int refundRequestCount;
        private BigDecimal requestedAmount;
        private BigDecimal expectedCancelledAmount;

        TestRefundService(
                PaymentRepository paymentRepository,
                RefundRepository refundRepository,
                PaymentStatusCodeRepository paymentStatusCodeRepository,
                RefundStatusCodeRepository refundStatusCodeRepository,
                RecordingTransactionManager transactionManager
        ) {
            super(paymentRepository, refundRepository, paymentStatusCodeRepository, refundStatusCodeRepository, transactionManager);
            this.transactionManager = transactionManager;
        }

        @Override
        public String getToken() {
            tokenRequestedOutsideTransaction = transactionManager.activeCount() == 0;
            return "token";
        }

        @Override
        protected void processRefundRequest(
                String accessToken,
                String merchantUid,
                BigDecimal amount,
                BigDecimal expectedCancelledAmount,
                String reason
        ) throws IOException {
            refundRequestedOutsideTransaction = transactionManager.activeCount() == 0;
            refundRequestCount++;
            requestedAmount = amount;
            this.expectedCancelledAmount = expectedCancelledAmount;
        }
    }
}
