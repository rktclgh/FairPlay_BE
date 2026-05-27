package com.fairing.fairplay.payment.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.payment.dto.AdminRefundListRequestDto;
import com.fairing.fairplay.payment.dto.AdminRefundListResponseDto;
import com.fairing.fairplay.payment.dto.RefundApprovalDto;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.PaymentTargetType;
import com.fairing.fairplay.payment.entity.Refund;
import com.fairing.fairplay.payment.entity.RefundStatusCode;
import com.fairing.fairplay.payment.repository.RefundRepository;
import com.fairing.fairplay.payment.repository.RefundStatusCodeRepository;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRefundServiceAuthorizationTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private RefundStatusCodeRepository refundStatusCodeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefundService refundService;

    private AdminRefundService adminRefundService;

    @BeforeEach
    void setUp() {
        adminRefundService = new AdminRefundService(
                refundRepository,
                refundStatusCodeRepository,
                userRepository,
                refundService
        );
    }

    @Test
    void hostApproveRejectOwnEventRefundsSucceed() {
        CustomUserDetails manager = user(100L, "EVENT_MANAGER");
        Refund approveRefund = refund(1L, payment(10L, event(1L, 100L)), status("APPROVED"));
        when(refundRepository.findById(1L)).thenReturn(Optional.of(approveRefund));

        assertThat(adminRefundService.approveRefund(1L, RefundApprovalDto.builder().build(), manager).getRefundId())
                .isEqualTo(1L);
        assertThat(approveRefund.getRefundStatusCode().getCode()).isEqualTo("APPROVED");
        verify(refundService).approveRefund(eq(1L), any(RefundApprovalDto.class), eq(manager));

        Refund rejectRefund = refund(2L, payment(11L, event(1L, 100L)), status("REQUESTED"));
        when(refundRepository.findById(2L)).thenReturn(Optional.of(rejectRefund));
        when(userRepository.findById(100L)).thenReturn(Optional.of(new Users(100L)));
        when(refundStatusCodeRepository.findByCode("REJECTED")).thenReturn(Optional.of(status("REJECTED")));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(adminRefundService.rejectRefund(2L, RefundApprovalDto.builder().build(), manager).getRefundId())
                .isEqualTo(2L);
        assertThat(rejectRefund.getRefundStatusCode().getCode()).isEqualTo("REJECTED");
    }

    @Test
    void immediateApproveDelegatesToRefundServiceSafePgFlow() {
        CustomUserDetails manager = user(100L, "EVENT_MANAGER");
        Refund refund = refund(1L, payment(10L, event(1L, 100L)), status("APPROVED"));
        RefundApprovalDto approval = RefundApprovalDto.builder()
                .processImmediately(true)
                .adminComment("즉시 환불")
                .build();
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));

        assertThat(adminRefundService.approveRefund(1L, approval, manager).getRefundId()).isEqualTo(1L);

        verify(refundService).approveRefund(1L, approval, manager);
        verify(refundService).recordRefundApprovalMetadata(1L, approval, manager);
        verify(refundStatusCodeRepository, never()).findByCode("PROCESSING");
        verify(refundStatusCodeRepository, never()).findByCode("COMPLETED");
    }

    @Test
    void hostApproveRejectOtherEventRefundsAreDeniedBeforeMutation() {
        CustomUserDetails manager = user(999L, "EVENT_MANAGER");
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(refundService).approveRefund(eq(1L), any(RefundApprovalDto.class), eq(manager));

        assertThatThrownBy(() -> adminRefundService.approveRefund(1L, RefundApprovalDto.builder().build(), manager))
                .isInstanceOf(AccessDeniedException.class);
        Refund refund = refund(1L, payment(10L, event(1L, 100L)), status("REQUESTED"));
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        assertThatThrownBy(() -> adminRefundService.rejectRefund(1L, RefundApprovalDto.builder().build(), manager))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(refund.getRefundStatusCode().getCode()).isEqualTo("REQUESTED");
        verify(refundRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
        verify(refundStatusCodeRepository, never()).findByCode("APPROVED");
        verify(refundStatusCodeRepository, never()).findByCode("REJECTED");
    }

    @Test
    void hostApproveRejectEventNullRefundsAreDeniedBeforeMutation() {
        CustomUserDetails manager = user(100L, "EVENT_MANAGER");
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(refundService).approveRefund(eq(1L), any(RefundApprovalDto.class), eq(manager));

        assertThatThrownBy(() -> adminRefundService.approveRefund(1L, RefundApprovalDto.builder().build(), manager))
                .isInstanceOf(AccessDeniedException.class);
        Refund refund = refund(1L, payment(10L, null), status("REQUESTED"));
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));
        assertThatThrownBy(() -> adminRefundService.rejectRefund(1L, RefundApprovalDto.builder().build(), manager))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(refund.getRefundStatusCode().getCode()).isEqualTo("REQUESTED");
        verify(refundRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void hostListWithoutEventIdScopesByManagerUserId() {
        CustomUserDetails manager = user(100L, "EVENT_MANAGER");
        AdminRefundListRequestDto request = defaultListRequest();
        when(refundRepository.findAdminRefundsWithFilters(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(100L), any(Pageable.class)))
                .thenReturn(Page.empty());

        assertThat(adminRefundService.getAdminRefundList(request, manager)).isEmpty();

        verify(refundRepository).findAdminRefundsWithFilters(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(100L), any(Pageable.class));
    }

    @Test
    void adminCanApproveEventNullRefundAndListWithoutManagerScope() {
        CustomUserDetails admin = user(1L, "ADMIN");
        Refund refund = refund(1L, payment(10L, null), status("APPROVED"));
        when(refundRepository.findById(1L)).thenReturn(Optional.of(refund));

        assertThat(adminRefundService.approveRefund(1L, RefundApprovalDto.builder().build(), admin).getRefundId())
                .isEqualTo(1L);
        assertThat(refund.getRefundStatusCode().getCode()).isEqualTo("APPROVED");

        AdminRefundListRequestDto request = defaultListRequest();
        when(refundRepository.findAdminRefundsWithFilters(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(Page.empty());

        assertThat(adminRefundService.getAdminRefundList(request, admin)).isEmpty();
    }

    private AdminRefundListRequestDto defaultListRequest() {
        return AdminRefundListRequestDto.builder()
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

    private Payment payment(Long paymentId, Event event) {
        return Payment.builder()
                .paymentId(paymentId)
                .event(event)
                .user(new Users(300L))
                .paymentTargetType(PaymentTargetType.builder()
                        .paymentTargetCode("RESERVATION")
                        .paymentTargetName("예약")
                        .build())
                .merchantUid("merchant-" + paymentId)
                .quantity(1)
                .price(BigDecimal.valueOf(100))
                .amount(BigDecimal.valueOf(100))
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
        return event;
    }

    private RefundStatusCode status(String code) {
        return RefundStatusCode.builder()
                .code(code)
                .name(code)
                .build();
    }

    private CustomUserDetails user(Long userId, String roleCode) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getUserId()).thenReturn(userId);
        lenient().when(userDetails.getRoleCode()).thenReturn(roleCode);
        return userDetails;
    }
}
