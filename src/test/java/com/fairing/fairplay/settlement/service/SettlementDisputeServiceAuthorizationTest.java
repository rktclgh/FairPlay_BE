package com.fairing.fairplay.settlement.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.core.service.LocalFileService;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.settlement.dto.SettlementDisputeDto;
import com.fairing.fairplay.settlement.entity.Settlement;
import com.fairing.fairplay.settlement.entity.SettlementDispute;
import com.fairing.fairplay.settlement.entity.SettlementDisputeFile;
import com.fairing.fairplay.settlement.repository.SettlementDisputeFileRepository;
import com.fairing.fairplay.settlement.repository.SettlementDisputeRepository;
import com.fairing.fairplay.settlement.repository.SettlementRepository;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementDisputeServiceAuthorizationTest {

    @Mock
    private SettlementDisputeRepository disputeRepository;

    @Mock
    private SettlementDisputeFileRepository disputeFileRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private LocalFileService localFileService;

    @Mock
    private UserRepository userRepository;

    private SettlementDisputeService service;

    @BeforeEach
    void setUp() {
        service = new SettlementDisputeService(
                disputeRepository,
                disputeFileRepository,
                settlementRepository,
                localFileService,
                userRepository
        );
    }

    @Test
    void foreignEventManagerSubmitIsDeniedBeforeSave() {
        Settlement settlement = settlement(7L, 100L);
        when(settlementRepository.findById(7L)).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.submitDispute(createRequest(7L), user(999L, "EVENT_MANAGER", "foreign")))
                .isInstanceOf(AccessDeniedException.class);

        verify(disputeRepository, never()).save(any());
    }

    @Test
    void ownerEventManagerSubmitIsAllowedAndRequesterComesFromPrincipal() {
        Settlement settlement = settlement(7L, 100L);
        when(settlementRepository.findById(7L)).thenReturn(Optional.of(settlement));
        when(disputeRepository.existsBySettlement_SettlementId(7L)).thenReturn(false);
        when(disputeRepository.save(any(SettlementDispute.class))).thenAnswer(invocation -> {
            SettlementDispute dispute = invocation.getArgument(0);
            dispute.setDisputeId(1L);
            return dispute;
        });
        when(settlementRepository.save(settlement)).thenReturn(settlement);

        service.submitDispute(createRequest(7L), user(100L, "EVENT_MANAGER", "owner"));

        ArgumentCaptor<SettlementDispute> disputeCaptor = ArgumentCaptor.forClass(SettlementDispute.class);
        verify(disputeRepository).save(disputeCaptor.capture());
        assertThat(disputeCaptor.getValue().getRequesterId()).isEqualTo(100L);
    }

    @Test
    void commonUserIsDeniedForDisputeSurfaces() {
        CustomUserDetails common = user(300L, "COMMON", "common");
        SettlementDispute dispute = dispute(1L, settlement(7L, 100L));
        SettlementDisputeFile file = disputeFile(10L, dispute);

        when(settlementRepository.findById(7L)).thenReturn(Optional.of(dispute.getSettlement()));
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeFileRepository.findById(10L)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> service.submitDispute(createRequest(7L), common))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getDispute(1L, common))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getDisputeBySettlementId(7L, common))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getAuthorizedFileKey(10L, common))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getDisputeList(PageRequest.of(0, 20), common))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ownerEventManagerCanReadOwnDetailSettlementAndDownloadButForeignManagerCannot() {
        SettlementDispute dispute = dispute(1L, settlement(7L, 100L));
        SettlementDisputeFile file = disputeFile(10L, dispute);
        dispute.getDisputeFiles().add(file);

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(settlementRepository.findById(7L)).thenReturn(Optional.of(dispute.getSettlement()));
        when(disputeRepository.findBySettlement_SettlementId(7L)).thenReturn(Optional.of(dispute));
        when(disputeFileRepository.findById(10L)).thenReturn(Optional.of(file));

        CustomUserDetails owner = user(100L, "EVENT_MANAGER", "owner");
        assertThat(service.getDispute(1L, owner).getDisputeId()).isEqualTo(1L);
        assertThat(service.getDisputeBySettlementId(7L, owner)).isPresent();
        assertThat(service.getAuthorizedFileKey(10L, owner)).isEqualTo("uploads/disputes/1/evidence.pdf");

        CustomUserDetails foreign = user(999L, "EVENT_MANAGER", "foreign");
        assertThatThrownBy(() -> service.getDispute(1L, foreign))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getDisputeBySettlementId(7L, foreign))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getAuthorizedFileKey(10L, foreign))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void responseNamesAreResolvedFromStoredPrincipalIds() {
        SettlementDispute dispute = SettlementDispute.builder()
                .disputeId(1L)
                .settlement(settlement(7L, 100L))
                .requesterId(100L)
                .adminId(1L)
                .disputeReason("정산 금액 확인이 필요합니다.")
                .status(SettlementDispute.DisputeProcessStatus.RESOLVED)
                .build();
        Users requester = new Users(100L);
        requester.setName("lookup owner");
        Users admin = new Users(1L);
        admin.setName("lookup admin");

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(userRepository.findById(100L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        SettlementDisputeDto.Response response = service.getDispute(1L, user(1L, "ADMIN", "admin"));

        assertThat(response.getRequesterName()).isEqualTo("lookup owner");
        assertThat(response.getAdminName()).isEqualTo("lookup admin");
    }

    @Test
    void bySettlementChecksOwnershipEvenWhenNoDisputeExists() {
        Settlement settlement = settlement(7L, 100L);
        when(settlementRepository.findById(7L)).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> service.getDisputeBySettlementId(7L, user(999L, "EVENT_MANAGER", "foreign")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCanListReviewDetailAndDownloadAndReviewAdminComesFromPrincipalId() {
        SettlementDispute dispute = dispute(1L, settlement(7L, 100L));
        SettlementDisputeFile file = disputeFile(10L, dispute);
        dispute.getDisputeFiles().add(file);
        CustomUserDetails admin = user(1L, "ADMIN", "admin");
        SettlementDisputeDto.AdminReviewRequest request = SettlementDisputeDto.AdminReviewRequest.builder()
                .disputeId(1L)
                .status(SettlementDispute.DisputeProcessStatus.RESOLVED)
                .adminResponse("확인 완료")
                .build();

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.findAllWithSettlement(PageRequest.of(0, 20))).thenReturn(Page.empty());
        when(disputeFileRepository.findById(10L)).thenReturn(Optional.of(file));
        when(disputeRepository.save(dispute)).thenReturn(dispute);
        when(settlementRepository.save(dispute.getSettlement())).thenReturn(dispute.getSettlement());

        assertThat(service.getDisputeList(PageRequest.of(0, 20), admin)).isEmpty();
        assertThat(service.getDispute(1L, admin).getDisputeId()).isEqualTo(1L);
        assertThat(service.getAuthorizedFileKey(10L, admin)).isEqualTo("uploads/disputes/1/evidence.pdf");

        service.reviewDispute(request, admin);

        assertThat(dispute.getAdminId()).isEqualTo(1L);
    }

    @Test
    void reviewResponseUsesUserLookupWhenAdminNameIsNeeded() {
        SettlementDispute dispute = dispute(1L, settlement(7L, 100L));
        CustomUserDetails admin = user(1L, "ADMIN", null);
        Users requester = new Users(100L);
        requester.setName("lookup owner");
        Users adminUser = new Users(1L);
        adminUser.setName("lookup admin");
        SettlementDisputeDto.AdminReviewRequest request = SettlementDisputeDto.AdminReviewRequest.builder()
                .disputeId(1L)
                .status(SettlementDispute.DisputeProcessStatus.UNDER_REVIEW)
                .build();

        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(userRepository.findById(100L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(disputeRepository.save(dispute)).thenReturn(dispute);
        when(settlementRepository.save(dispute.getSettlement())).thenReturn(dispute.getSettlement());

        SettlementDisputeDto.Response response = service.reviewDispute(request, admin);

        assertThat(dispute.getAdminId()).isEqualTo(1L);
        assertThat(response.getAdminName()).isEqualTo("lookup admin");
    }

    @Test
    void reviewIsAdminOnly() {
        SettlementDispute dispute = dispute(1L, settlement(7L, 100L));
        SettlementDisputeDto.AdminReviewRequest request = SettlementDisputeDto.AdminReviewRequest.builder()
                .disputeId(1L)
                .status(SettlementDispute.DisputeProcessStatus.RESOLVED)
                .build();
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        assertThatThrownBy(() -> service.reviewDispute(request, user(100L, "EVENT_MANAGER", "owner")))
                .isInstanceOf(AccessDeniedException.class);

        verify(disputeRepository, never()).save(any());
    }

    private SettlementDisputeDto.CreateRequest createRequest(Long settlementId) {
        return SettlementDisputeDto.CreateRequest.builder()
                .settlementId(settlementId)
                .disputeReason("정산 금액 확인이 필요합니다.")
                .build();
    }

    private SettlementDispute dispute(Long disputeId, Settlement settlement) {
        return SettlementDispute.builder()
                .disputeId(disputeId)
                .settlement(settlement)
                .requesterId(100L)
                .disputeReason("정산 금액 확인이 필요합니다.")
                .status(SettlementDispute.DisputeProcessStatus.RAISED)
                .build();
    }

    private SettlementDisputeFile disputeFile(Long fileId, SettlementDispute dispute) {
        return SettlementDisputeFile.builder()
                .fileId(fileId)
                .dispute(dispute)
                .s3Key("uploads/disputes/1/evidence.pdf")
                .originalFilename("evidence.pdf")
                .fileSize(123L)
                .fileType(SettlementDisputeFile.DisputeFileType.PDF)
                .uploadOrder(1)
                .build();
    }

    private Settlement settlement(Long settlementId, Long managerUserId) {
        EventAdmin eventAdmin = new EventAdmin();
        eventAdmin.setUserId(managerUserId);
        eventAdmin.setUser(new Users(managerUserId));

        Event event = new Event();
        event.setEventId(70L);
        event.setManager(eventAdmin);

        return Settlement.builder()
                .settlementId(settlementId)
                .event(event)
                .eventTitle("정산 행사")
                .totalAmount(BigDecimal.valueOf(10000))
                .feeAmount(BigDecimal.valueOf(1000))
                .finalAmount(BigDecimal.valueOf(9000))
                .build();
    }

    private CustomUserDetails user(Long userId, String roleCode, String name) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getUserId()).thenReturn(userId);
        lenient().when(userDetails.getRoleCode()).thenReturn(roleCode);
        lenient().when(userDetails.getName()).thenReturn(name);
        return userDetails;
    }
}
