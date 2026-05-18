package com.fairing.fairplay.settlement.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.core.service.LocalFileService;
import com.fairing.fairplay.settlement.dto.SettlementDisputeDto;
import com.fairing.fairplay.settlement.entity.SettlementDispute;
import com.fairing.fairplay.settlement.service.SettlementDisputeService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementDisputeControllerPrincipalTest {

    @Mock
    private SettlementDisputeService disputeService;

    @Mock
    private LocalFileService localFileService;

    private SettlementDisputeController controller;

    @BeforeEach
    void setUp() {
        controller = new SettlementDisputeController(disputeService, localFileService);
    }

    @Test
    void submitDisputeUsesAuthenticationPrincipal() {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        SettlementDisputeDto.CreateRequest request = SettlementDisputeDto.CreateRequest.builder()
                .settlementId(7L)
                .disputeReason("정산 금액 확인")
                .build();
        SettlementDisputeDto.Response response = SettlementDisputeDto.Response.builder()
                .disputeId(1L)
                .settlementId(7L)
                .build();
        when(disputeService.submitDispute(request, principal)).thenReturn(response);

        var result = controller.submitDispute(request, principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(disputeService).submitDispute(request, principal);
    }

    @Test
    void reviewDisputeUsesAuthenticationPrincipal() {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        SettlementDisputeDto.AdminReviewRequest request = SettlementDisputeDto.AdminReviewRequest.builder()
                .disputeId(1L)
                .status(SettlementDispute.DisputeProcessStatus.RESOLVED)
                .adminResponse("확인")
                .build();
        SettlementDisputeDto.Response response = SettlementDisputeDto.Response.builder()
                .disputeId(1L)
                .build();
        when(disputeService.reviewDispute(request, principal)).thenReturn(response);

        var result = controller.reviewDispute(request, principal);

        assertThat(result.getBody()).isSameAs(response);
        verify(disputeService).reviewDispute(request, principal);
    }

    @Test
    void downloadDisputeFileGetsAuthorizedKeyFromPrincipalBeforeDownload() throws IOException {
        CustomUserDetails principal = mock(CustomUserDetails.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(disputeService.getAuthorizedFileKey(10L, principal)).thenReturn("uploads/disputes/1/evidence.pdf");

        controller.downloadDisputeFile(10L, principal, response);

        verify(disputeService).getAuthorizedFileKey(10L, principal);
        verify(localFileService).downloadFile("uploads/disputes/1/evidence.pdf", response);
    }
}
