package com.fairing.fairplay.payment.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.payment.dto.PaymentSearchCriteria;
import com.fairing.fairplay.payment.repository.PaymentAdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentAdminServiceAuthorizationTest {

    @Mock
    private PaymentAdminRepository paymentAdminRepository;

    private PaymentAdminService paymentAdminService;

    @BeforeEach
    void setUp() {
        paymentAdminService = new PaymentAdminService(paymentAdminRepository);
    }

    @Test
    void adminListsWithoutManagerScope() {
        PaymentSearchCriteria criteria = defaultCriteria();
        when(paymentAdminRepository.findPaymentsWithCriteria(eq(criteria), eq(null), any(Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        paymentAdminService.getPaymentList(criteria, user(1L, "ADMIN"));

        verify(paymentAdminRepository).findPaymentsWithCriteria(eq(criteria), eq(null), any(Pageable.class));
    }

    @Test
    void eventManagerListsWithOwnManagerScope() {
        PaymentSearchCriteria criteria = defaultCriteria();
        when(paymentAdminRepository.findPaymentsWithCriteria(eq(criteria), eq(100L), any(Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        paymentAdminService.getPaymentList(criteria, user(100L, "EVENT_MANAGER"));

        verify(paymentAdminRepository).findPaymentsWithCriteria(eq(criteria), eq(100L), any(Pageable.class));
    }

    @Test
    void commonIsDeniedBeforeRepositoryCall() {
        PaymentSearchCriteria criteria = defaultCriteria();

        assertThatThrownBy(() -> paymentAdminService.getPaymentList(criteria, user(300L, "COMMON")))
                .isInstanceOf(AccessDeniedException.class);

        verify(paymentAdminRepository, never()).findPaymentsWithCriteria(any(), any(), any());
    }

    private PaymentSearchCriteria defaultCriteria() {
        PaymentSearchCriteria criteria = new PaymentSearchCriteria();
        criteria.setPage(0);
        criteria.setSize(20);
        criteria.setSort("paidAt");
        criteria.setDirection("desc");
        return criteria;
    }

    private CustomUserDetails user(Long userId, String roleCode) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getRoleCode()).thenReturn(roleCode);
        return userDetails;
    }
}
