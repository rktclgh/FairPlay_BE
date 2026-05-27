package com.fairing.fairplay.payment.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.payment.dto.PaymentRequestDto;
import com.fairing.fairplay.payment.dto.RefundApprovalDto;
import com.fairing.fairplay.payment.dto.RefundListRequestDto;
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
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RefundServiceAuthorizationTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentStatusCodeRepository paymentStatusCodeRepository;

    @Mock
    private RefundStatusCodeRepository refundStatusCodeRepository;

    private TestRefundService refundService;

    @BeforeEach
    void setUp() {
        refundService = new TestRefundService(
                paymentRepository,
                refundRepository,
                paymentStatusCodeRepository,
                refundStatusCodeRepository
        );
    }

    @Test
    void commonUserCannotUseAdminRefundEndpointsBeforeMutationOrBroadQuery() {
        CustomUserDetails common = user(300L, "COMMON");

        assertThatThrownBy(() -> refundService.approveRefund(1L, RefundApprovalDto.builder().build(), common))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> refundService.rejectRefund(1L, "거절", common))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> refundService.getAllRefunds(null, common))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> refundService.getPendingRefunds(null, common))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> refundService.getRefundList(defaultListRequest(), common))
                .isInstanceOf(AccessDeniedException.class);

        verify(refundRepository, never()).findById(any());
        verify(refundRepository, never()).findAll();
        verify(refundRepository, never()).findByRefundStatusCode(any());
        verify(refundRepository, never()).findRefundsWithFilters(any(), any(), any(), any(), any(), any(), any());
        verify(refundRepository, never()).save(any());
    }

    @Test
    void commonUserCanRequestAndReadOwnRefunds() {
        Payment payment = payment(10L, 300L, null);
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(refundStatusCodeRepository.findByCode("REQUESTED")).thenReturn(Optional.of(status("REQUESTED")));

        PaymentRequestDto request = new PaymentRequestDto();
        request.setRefundRequestAmount(BigDecimal.TEN);
        request.setReason("개인 사유");

        assertThat(refundService.requestRefund(10L, request, 300L).getPaymentId()).isEqualTo(10L);
        verify(refundRepository).save(any(Refund.class));

        Refund refund = refund(1L, payment, status("REQUESTED"));
        when(refundRepository.findByUserId(300L)).thenReturn(List.of(refund));

        assertThat(refundService.getMyRefundList(300L)).hasSize(1);
    }

    @Test
    void adminCanApproveRejectAndListAnyRefund() {
        Refund refundForApprove = refund(1L, payment(10L, 300L, event(1L, 100L)), status("REQUESTED"));
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refundForApprove));
        when(refundStatusCodeRepository.findByCode("APPROVED")).thenReturn(Optional.of(status("APPROVED")));
        when(paymentStatusCodeRepository.getReferenceById(5)).thenReturn(paymentStatus(5));

        assertThat(refundService.approveRefund(1L, RefundApprovalDto.builder().build(), user(1L, "ADMIN")).getPaymentId())
                .isEqualTo(10L);
        assertThat(refundForApprove.getRefundStatusCode().getCode()).isEqualTo("APPROVED");

        Refund refundForReject = refund(2L, payment(11L, 301L, null), status("REQUESTED"));
        when(refundRepository.findById(2L)).thenReturn(Optional.of(refundForReject));
        when(refundStatusCodeRepository.findByCode("REJECTED")).thenReturn(Optional.of(status("REJECTED")));

        assertThat(refundService.rejectRefund(2L, "거절", user(1L, "ADMIN")).getPaymentId()).isEqualTo(11L);
        assertThat(refundForReject.getRefundStatusCode().getCode()).isEqualTo("REJECTED");

        when(refundRepository.findAll()).thenReturn(List.of(refundForApprove, refundForReject));
        assertThat(refundService.getAllRefunds(null, user(1L, "ADMIN"))).hasSize(2);
    }

    @Test
    void eventManagerCanApproveRejectAndListOwnEventRefunds() {
        CustomUserDetails manager = user(100L, "EVENT_MANAGER");
        Refund refundForApprove = refund(1L, payment(10L, 300L, event(1L, 100L)), status("REQUESTED"));
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refundForApprove));
        when(refundStatusCodeRepository.findByCode("APPROVED")).thenReturn(Optional.of(status("APPROVED")));
        when(paymentStatusCodeRepository.getReferenceById(5)).thenReturn(paymentStatus(5));

        assertThat(refundService.approveRefund(1L, RefundApprovalDto.builder().build(), manager).getPaymentId())
                .isEqualTo(10L);
        assertThat(refundForApprove.getRefundStatusCode().getCode()).isEqualTo("APPROVED");

        Refund refundForReject = refund(2L, payment(11L, 301L, event(1L, 100L)), status("REQUESTED"));
        when(refundRepository.findById(2L)).thenReturn(Optional.of(refundForReject));
        when(refundStatusCodeRepository.findByCode("REJECTED")).thenReturn(Optional.of(status("REJECTED")));

        assertThat(refundService.rejectRefund(2L, "거절", manager).getPaymentId()).isEqualTo(11L);
        assertThat(refundForReject.getRefundStatusCode().getCode()).isEqualTo("REJECTED");

        when(refundRepository.findByEventManagerUserId(100L)).thenReturn(List.of(refundForApprove));
        assertThat(refundService.getAllRefunds(null, manager)).hasSize(1);

        when(refundRepository.findRefundsWithFilters(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(100L), eq(PageRequest.of(0, 20,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))))
                .thenReturn(Page.empty());
        assertThat(refundService.getRefundList(defaultListRequest(), manager)).isEmpty();
    }

    @Test
    void eventManagerCannotMutateOtherEventRefund() {
        Refund refund = refund(1L, payment(10L, 300L, event(1L, 100L)), status("REQUESTED"));
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));

        assertThatThrownBy(() -> refundService.approveRefund(1L, RefundApprovalDto.builder().build(), user(999L, "EVENT_MANAGER")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> refundService.rejectRefund(1L, "거절", user(999L, "EVENT_MANAGER")))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(refund.getRefundStatusCode().getCode()).isEqualTo("REQUESTED");
        verify(refundRepository, never()).save(any());
        verify(refundStatusCodeRepository, never()).findByCode("APPROVED");
        verify(refundStatusCodeRepository, never()).findByCode("REJECTED");
    }

    @Test
    void eventManagerCannotMutateRefundWithoutEvent() {
        Refund refund = refund(1L, payment(10L, 300L, null), status("REQUESTED"));
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));

        assertThatThrownBy(() -> refundService.approveRefund(1L, RefundApprovalDto.builder().build(), user(100L, "EVENT_MANAGER")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> refundService.rejectRefund(1L, "거절", user(100L, "EVENT_MANAGER")))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(refund.getRefundStatusCode().getCode()).isEqualTo("REQUESTED");
        verify(refundRepository, never()).save(any());
    }

    @Test
    void eventManagerPendingListIsScopedToOwnEventsBeforeBroadStatusQuery() {
        RefundStatusCode requested = status("REQUESTED");
        when(refundStatusCodeRepository.findByCode("REQUESTED")).thenReturn(Optional.of(requested));
        when(refundRepository.findByEventManagerUserIdAndRefundStatusCode(100L, requested)).thenReturn(List.of());

        assertThat(refundService.getPendingRefunds(null, user(100L, "EVENT_MANAGER"))).isEmpty();

        verify(refundRepository, never()).findByRefundStatusCode(any());
    }

    @Test
    void eventManagerTargetedOtherEventListIsDeniedAfterScopedLookup() {
        Refund otherEventRefund = refund(1L, payment(10L, 300L, event(1L, 100L)), status("REQUESTED"));
        when(refundRepository.findByEventId(1L)).thenReturn(List.of(otherEventRefund));

        assertThatThrownBy(() -> refundService.getAllRefunds(1L, user(999L, "EVENT_MANAGER")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void eventManagerListFiltersByManagerInsteadOfBroadQuery() {
        CustomUserDetails manager = user(100L, "EVENT_MANAGER");
        RefundListRequestDto request = defaultListRequest();
        when(refundRepository.findRefundsWithFilters(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(100L), any(PageRequest.class)))
                .thenReturn(Page.empty());

        assertThatCode(() -> refundService.getRefundList(request, manager)).doesNotThrowAnyException();
    }

    private RefundListRequestDto defaultListRequest() {
        return RefundListRequestDto.builder()
                .page(0)
                .size(20)
                .sortBy("createdAt")
                .sortDirection("desc")
                .build();
    }

    private Refund refund(Long refundId, Payment payment, RefundStatusCode status) {
        return Refund.builder()
                .refundId(refundId)
                .payment(payment)
                .amount(BigDecimal.TEN)
                .reason("환불 사유")
                .refundStatusCode(status)
                .build();
    }

    private Payment payment(Long paymentId, Long userId, Event event) {
        return Payment.builder()
                .paymentId(paymentId)
                .event(event)
                .user(new Users(userId))
                .paymentTargetType(PaymentTargetType.builder()
                        .paymentTargetCode("RESERVATION")
                        .paymentTargetName("예약")
                        .build())
                .targetId(20L)
                .merchantUid("merchant-" + paymentId)
                .impUid("imp-" + paymentId)
                .quantity(1)
                .price(BigDecimal.valueOf(100))
                .amount(BigDecimal.valueOf(100))
                .refundedAmount(BigDecimal.ZERO)
                .paymentTypeCode(PaymentTypeCode.builder().paymentTypeCodeId(1).code("CARD").name("카드").build())
                .paymentStatusCode(paymentStatus(2))
                .build();
    }

    private Event event(Long eventId, Long managerUserId) {
        EventAdmin eventAdmin = new EventAdmin();
        eventAdmin.setUserId(managerUserId);
        eventAdmin.setUser(new Users(managerUserId));

        Event event = new Event();
        event.setEventId(eventId);
        event.setTitleKr("행사");
        event.setTitleEng("Event");
        event.setManager(eventAdmin);

        EventDetail detail = new EventDetail();
        detail.setStartDate(LocalDate.now());
        detail.setEndDate(LocalDate.now().plusDays(1));
        event.setEventDetail(detail);
        return event;
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

    private CustomUserDetails user(Long userId, String roleCode) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getRoleCode()).thenReturn(roleCode);
        return userDetails;
    }

    private static class TestRefundService extends RefundService {

        TestRefundService(
                PaymentRepository paymentRepository,
                RefundRepository refundRepository,
                PaymentStatusCodeRepository paymentStatusCodeRepository,
                RefundStatusCodeRepository refundStatusCodeRepository
        ) {
            super(paymentRepository, refundRepository, paymentStatusCodeRepository, refundStatusCodeRepository);
        }

        @Override
        public String getToken() {
            return "token";
        }

        @Override
        protected void processRefundRequest(String accessToken, String merchantUid, BigDecimal amount, String reason) throws IOException {
            // Unit tests assert authorization and mutation boundaries without calling the external PG API.
        }
    }
}
