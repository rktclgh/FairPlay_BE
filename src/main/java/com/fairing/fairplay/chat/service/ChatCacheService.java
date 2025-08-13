package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Redis 키 패턴
    private static final String CHAT_ROOM_MESSAGES_KEY = "chat:room:%d:messages";
    private static final String CHAT_ROOM_LIST_KEY = "chat:user:%d:rooms";
    private static final String CHAT_MESSAGE_KEY = "chat:message:%d";
    
    /**
     * 채팅방의 메시지를 Redis에 캐싱 (최근 100개만 유지)
     */
    public void cacheMessage(Long roomId, ChatMessageResponseDto message) {
        try {
            String key = String.format(CHAT_ROOM_MESSAGES_KEY, roomId);
            String messageJson = objectMapper.writeValueAsString(message);
            
            // List에 추가 (최신 메시지가 뒤에)
            redisTemplate.opsForList().rightPush(key, messageJson);
            
            // 최근 100개만 유지
            redisTemplate.opsForList().trim(key, -100, -1);
            
            // 1시간 TTL 설정
            redisTemplate.expire(key, Duration.ofHours(1));
            
            log.debug("메시지 캐싱 완료: roomId={}, messageId={}", roomId, message.getChatMessageId());
        } catch (JsonProcessingException e) {
            log.error("메시지 캐싱 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 채팅방의 캐싱된 메시지 조회
     */
    public List<ChatMessageResponseDto> getCachedMessages(Long roomId) {
        try {
            String key = String.format(CHAT_ROOM_MESSAGES_KEY, roomId);
            List<String> messageJsons = redisTemplate.opsForList().range(key, 0, -1);
            
            List<ChatMessageResponseDto> messages = new ArrayList<>();
            if (messageJsons != null) {
                for (String messageJson : messageJsons) {
                    try {
                        ChatMessageResponseDto message = objectMapper.readValue(messageJson, ChatMessageResponseDto.class);
                        messages.add(message);
                    } catch (JsonProcessingException e) {
                        log.warn("캐싱된 메시지 파싱 실패: {}", e.getMessage());
                    }
                }
            }
            
            log.debug("캐싱된 메시지 조회: roomId={}, count={}", roomId, messages.size());
            return messages;
        } catch (Exception e) {
            log.error("캐싱된 메시지 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 캐싱된 메시지가 있는지 확인
     */
    public boolean hasCachedMessages(Long roomId) {
        String key = String.format(CHAT_ROOM_MESSAGES_KEY, roomId);
        Long size = redisTemplate.opsForList().size(key);
        return size != null && size > 0;
    }
    
    /**
     * 사용자의 채팅방 목록 캐싱
     */
    public void cacheUserRooms(Long userId, List<Long> roomIds) {
        try {
            String key = String.format(CHAT_ROOM_LIST_KEY, userId);
            redisTemplate.delete(key);
            
            if (!roomIds.isEmpty()) {
                for (Long roomId : roomIds) {
                    redisTemplate.opsForSet().add(key, roomId.toString());
                }
                // 10분 TTL
                redisTemplate.expire(key, Duration.ofMinutes(10));
            }
            
            log.debug("사용자 채팅방 목록 캐싱: userId={}, rooms={}", userId, roomIds.size());
        } catch (Exception e) {
            log.error("사용자 채팅방 목록 캐싱 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 사용자의 캐싱된 채팅방 목록 조회
     */
    public List<Long> getCachedUserRooms(Long userId) {
        try {
            String key = String.format(CHAT_ROOM_LIST_KEY, userId);
            Set<String> roomIdStrings = redisTemplate.opsForSet().members(key);
            
            List<Long> roomIds = new ArrayList<>();
            if (roomIdStrings != null) {
                for (String roomIdString : roomIdStrings) {
                    try {
                        roomIds.add(Long.parseLong(roomIdString));
                    } catch (NumberFormatException e) {
                        log.warn("잘못된 채팅방 ID 형식: {}", roomIdString);
                    }
                }
            }
            
            log.debug("캐싱된 사용자 채팅방 목록 조회: userId={}, rooms={}", userId, roomIds.size());
            return roomIds;
        } catch (Exception e) {
            log.error("캐싱된 사용자 채팅방 목록 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 채팅방 메시지 캐시 무효화
     */
    public void invalidateRoomMessages(Long roomId) {
        String key = String.format(CHAT_ROOM_MESSAGES_KEY, roomId);
        redisTemplate.delete(key);
        log.debug("채팅방 메시지 캐시 무효화: roomId={}", roomId);
    }
    
    /**
     * 사용자 채팅방 목록 캐시 무효화
     */
    public void invalidateUserRooms(Long userId) {
        String key = String.format(CHAT_ROOM_LIST_KEY, userId);
        redisTemplate.delete(key);
        log.debug("사용자 채팅방 목록 캐시 무효화: userId={}", userId);
    }
    
    /**
     * 채팅방의 읽지 않은 메시지 수 캐싱
     */
    public void cacheUnreadCount(Long roomId, Long userId, Long count) {
        String key = String.format("chat:unread:%d:%d", roomId, userId);
        redisTemplate.opsForValue().set(key, count.toString(), Duration.ofMinutes(30));
    }
    
    /**
     * 캐싱된 읽지 않은 메시지 수 조회
     */
    public Long getCachedUnreadCount(Long roomId, Long userId) {
        String key = String.format("chat:unread:%d:%d", roomId, userId);
        String countString = redisTemplate.opsForValue().get(key);
        if (countString != null) {
            try {
                return Long.parseLong(countString);
            } catch (NumberFormatException e) {
                log.warn("잘못된 읽지 않은 메시지 수 형식: {}", countString);
            }
        }
        return null;
    }
    
    /**
     * 특정 채팅방의 모든 사용자 읽지 않은 메시지 캐시 무효화
     */
    public void invalidateUnreadCachesForRoom(Long roomId) {
        try {
            String pattern = String.format("chat:unread:%d:*", roomId);
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("채팅방 {} 읽지 않은 메시지 캐시 무효화: {} 개 키 삭제", roomId, keys.size());
            }
        } catch (Exception e) {
            log.error("채팅방 {} 읽지 않은 메시지 캐시 무효화 실패: {}", roomId, e.getMessage());
        }
    }
}