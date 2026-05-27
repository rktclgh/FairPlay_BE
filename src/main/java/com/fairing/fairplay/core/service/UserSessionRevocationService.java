package com.fairing.fairplay.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionRevocationService {

    private final SessionService sessionService;
    private final RefreshTokenService refreshTokenService;

    public void revokeAfterCommit(Long userId) {
        if (userId == null) {
            return;
        }

        sessionService.blockUserSessions(userId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            revokeNow(userId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                revokeNow(userId);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    sessionService.unblockUserSessions(userId);
                }
            }
        });
    }

    private void revokeNow(Long userId) {
        try {
            sessionService.deleteAllUserSessions(userId);
            refreshTokenService.deleteRefreshToken(userId);
            sessionService.unblockUserSessions(userId);
        } catch (RuntimeException e) {
            log.error("사용자 세션 폐기 실패 - userId: {}, blocked marker retained until TTL for fail-closed auth", userId, e);
            throw e;
        }
    }
}
