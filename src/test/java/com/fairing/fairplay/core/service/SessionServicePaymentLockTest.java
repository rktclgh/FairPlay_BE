package com.fairing.fairplay.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServicePaymentLockTest {

    private static final Duration PAYMENT_LOCK_TIMEOUT = Duration.ofMinutes(15);

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        sessionService = new SessionService(redisTemplate, new ObjectMapper());
    }

    @Test
    void setPaymentLockStoresUserLockAndMerchantIndexWithSameTtl() {
        when(valueOperations.get("payment_lock:42")).thenReturn(null);
        when(valueOperations.setIfAbsent(eq("payment_lock:42"), anyString(), eq(PAYMENT_LOCK_TIMEOUT)))
                .thenReturn(true);

        boolean locked = sessionService.setPaymentLock(42L, "merchant-42");

        assertThat(locked).isTrue();
        verify(valueOperations).setIfAbsent(eq("payment_lock:42"), anyString(), eq(PAYMENT_LOCK_TIMEOUT));
        verify(valueOperations).set("payment_lock_merchant:merchant-42", "42", PAYMENT_LOCK_TIMEOUT);
    }

    @Test
    void isPaymentLockedReturnsTrueForValidUserLock() throws Exception {
        String lockJson = new ObjectMapper().writeValueAsString(Map.of(
                "merchantUid", "merchant-42",
                "startedAt", System.currentTimeMillis(),
                "userId", 42L
        ));
        when(valueOperations.get("payment_lock:42")).thenReturn(lockJson);

        assertThat(sessionService.isPaymentLocked(42L)).isTrue();
    }

    @Test
    void clearPaymentLockDeletesUserLockAndMerchantIndex() throws Exception {
        String lockJson = new ObjectMapper().writeValueAsString(Map.of(
                "merchantUid", "merchant-42",
                "startedAt", System.currentTimeMillis(),
                "userId", 42L
        ));
        when(valueOperations.get("payment_lock:42")).thenReturn(lockJson);

        sessionService.clearPaymentLock(42L);

        verify(redisTemplate).delete("payment_lock:42");
        verify(redisTemplate).delete("payment_lock_merchant:merchant-42");
    }

    @Test
    void clearPaymentLockByMerchantUidDeletesIndexedUserLockWithoutScanningKeys() throws Exception {
        String lockJson = new ObjectMapper().writeValueAsString(Map.of(
                "merchantUid", "merchant-42",
                "startedAt", System.currentTimeMillis(),
                "userId", 42L
        ));
        when(valueOperations.get("payment_lock_merchant:merchant-42")).thenReturn("42");
        when(valueOperations.get("payment_lock:42")).thenReturn(lockJson);

        sessionService.clearPaymentLockByMerchantUid("merchant-42");

        verify(redisTemplate).delete("payment_lock:42");
        verify(redisTemplate).delete("payment_lock_merchant:merchant-42");
        verify(redisTemplate, never()).keys(anyString());
    }

    @Test
    void clearPaymentLockByMerchantUidRemovesOnlyStaleIndexWhenUserLockBelongsToDifferentMerchant() throws Exception {
        String lockJson = new ObjectMapper().writeValueAsString(Map.of(
                "merchantUid", "merchant-new",
                "startedAt", System.currentTimeMillis(),
                "userId", 42L
        ));
        when(valueOperations.get("payment_lock_merchant:merchant-old")).thenReturn("42");
        when(valueOperations.get("payment_lock:42")).thenReturn(lockJson);

        sessionService.clearPaymentLockByMerchantUid("merchant-old");

        verify(redisTemplate, never()).delete("payment_lock:42");
        verify(redisTemplate).delete("payment_lock_merchant:merchant-old");
        verify(redisTemplate, never()).keys(anyString());
    }

    @Test
    void clearPaymentLockByMerchantUidMissingIndexIsNoopWithoutScanningKeys() {
        when(valueOperations.get("payment_lock_merchant:missing")).thenReturn(null);

        sessionService.clearPaymentLockByMerchantUid("missing");

        verify(redisTemplate, never()).delete(anyString());
        verify(redisTemplate, never()).keys(anyString());
    }

    @Test
    void setPaymentLockStoresMerchantUidInUserLockPayload() throws Exception {
        when(valueOperations.get("payment_lock:42")).thenReturn(null);
        when(valueOperations.setIfAbsent(eq("payment_lock:42"), anyString(), eq(PAYMENT_LOCK_TIMEOUT)))
                .thenReturn(true);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        sessionService.setPaymentLock(42L, "merchant-42");

        verify(valueOperations).setIfAbsent(eq("payment_lock:42"), payloadCaptor.capture(), eq(PAYMENT_LOCK_TIMEOUT));
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = new ObjectMapper().readValue(payloadCaptor.getValue(), Map.class);
        assertThat(payload).containsEntry("merchantUid", "merchant-42");
        assertThat(((Number) payload.get("userId")).longValue()).isEqualTo(42L);
    }
}
