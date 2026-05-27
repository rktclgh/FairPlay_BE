package com.fairing.fairplay.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReservationPaymentIntentStore {

    private static final String KEY_PREFIX = "payment:intent:reservation:";
    private static final Duration INTENT_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(ReservationPaymentIntent intent) {
        try {
            redisTemplate.opsForValue().set(key(intent.merchantUid(), intent.userId()),
                    objectMapper.writeValueAsString(intent),
                    INTENT_TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("예약 결제 intent 저장에 실패했습니다.", e);
        }
    }

    public Optional<ReservationPaymentIntent> find(String merchantUid, Long userId) {
        String raw = redisTemplate.opsForValue().get(key(merchantUid, userId));
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, ReservationPaymentIntent.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("예약 결제 intent를 읽을 수 없습니다.", e);
        }
    }

    public void delete(String merchantUid, Long userId) {
        redisTemplate.delete(key(merchantUid, userId));
    }

    private String key(String merchantUid, Long userId) {
        return KEY_PREFIX + merchantUid + ":" + userId;
    }
}
