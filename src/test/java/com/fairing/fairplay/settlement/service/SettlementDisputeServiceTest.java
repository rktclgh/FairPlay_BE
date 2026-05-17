package com.fairing.fairplay.settlement.service;

import com.fairing.fairplay.core.service.LocalFileService;
import com.fairing.fairplay.settlement.dto.SettlementDisputeDto;
import com.fairing.fairplay.settlement.entity.Settlement;
import com.fairing.fairplay.settlement.entity.SettlementDispute;
import com.fairing.fairplay.settlement.entity.SettlementDisputeFile;
import com.fairing.fairplay.settlement.repository.SettlementDisputeFileRepository;
import com.fairing.fairplay.settlement.repository.SettlementDisputeRepository;
import com.fairing.fairplay.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementDisputeServiceTest {

    @Mock
    private SettlementDisputeRepository disputeRepository;

    @Mock
    private SettlementDisputeFileRepository disputeFileRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private LocalFileService localFileService;

    private SettlementDisputeService service;

    @BeforeEach
    void setUp() {
        service = new SettlementDisputeService(
                disputeRepository,
                disputeFileRepository,
                settlementRepository,
                localFileService
        );
    }

    @Test
    void submitDisputeAcceptsPrivateTempUploadKeyAndMovesFileToPermanentStorage() {
        String tempKey = "private/tmp/2026-05-18/evidence.pdf";
        String permanentKey = "uploads/disputes/900/evidence.pdf";
        Settlement settlement = settlement();
        SettlementDisputeDto.CreateRequest request = SettlementDisputeDto.CreateRequest.builder()
                .settlementId(7L)
                .disputeReason("정산 금액 증빙이 필요합니다.")
                .tempFileKeys(List.of(tempKey))
                .build();

        when(settlementRepository.findById(7L)).thenReturn(Optional.of(settlement));
        when(disputeRepository.existsBySettlement_SettlementId(7L)).thenReturn(false);
        when(localFileService.fileExists(tempKey)).thenReturn(true);
        lenient().when(disputeRepository.save(any(SettlementDispute.class))).thenAnswer(invocation -> {
            SettlementDispute dispute = invocation.getArgument(0);
            dispute.setDisputeId(900L);
            return dispute;
        });
        lenient().when(localFileService.moveToPermanent(tempKey, "disputes/900")).thenReturn(permanentKey);
        lenient().when(disputeFileRepository.save(any(SettlementDisputeFile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(settlementRepository.save(settlement)).thenReturn(settlement);

        assertThatCode(() -> service.submitDispute(request, 55L))
                .doesNotThrowAnyException();

        verify(localFileService).moveToPermanent(tempKey, "disputes/900");
        ArgumentCaptor<SettlementDisputeFile> fileCaptor = ArgumentCaptor.forClass(SettlementDisputeFile.class);
        verify(disputeFileRepository).save(fileCaptor.capture());
        assertThat(fileCaptor.getValue().getS3Key()).isEqualTo(permanentKey);
        assertThat(fileCaptor.getValue().getOriginalFilename()).isEqualTo("evidence.pdf");
        assertThat(fileCaptor.getValue().getUploadOrder()).isEqualTo(1);
    }

    private Settlement settlement() {
        return Settlement.builder()
                .settlementId(7L)
                .eventTitle("정산 행사")
                .totalAmount(BigDecimal.valueOf(10000))
                .feeAmount(BigDecimal.valueOf(1000))
                .finalAmount(BigDecimal.valueOf(9000))
                .build();
    }
}
