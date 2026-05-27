package com.fairing.fairplay.core.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserSessionRevocationServiceTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void blocksImmediatelyAndRevokesAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        UserSessionRevocationService service =
                new UserSessionRevocationService(sessionService, refreshTokenService);

        service.revokeAfterCommit(42L);

        verify(sessionService).blockUserSessions(42L);
        verifyNoInteractions(refreshTokenService);

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);

        synchronizations.get(0).afterCommit();

        InOrder inOrder = inOrder(sessionService, refreshTokenService);
        inOrder.verify(sessionService).deleteAllUserSessions(42L);
        inOrder.verify(refreshTokenService).deleteRefreshToken(42L);
        inOrder.verify(sessionService).unblockUserSessions(42L);
    }

    @Test
    void unblocksWithoutRevokingWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        UserSessionRevocationService service =
                new UserSessionRevocationService(sessionService, refreshTokenService);

        service.revokeAfterCommit(42L);
        TransactionSynchronizationManager.getSynchronizations()
                .get(0)
                .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(sessionService).blockUserSessions(42L);
        verify(sessionService).unblockUserSessions(42L);
        verifyNoInteractions(refreshTokenService);
    }
}
