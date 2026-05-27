package com.fairing.fairplay.chat.controller;

import com.fairing.fairplay.chat.dto.ChatMessagePageResponseDto;
import com.fairing.fairplay.chat.dto.ChatMessageRequestDto;
import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.dto.ChatRoomResponseDto;
import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import com.fairing.fairplay.chat.service.ChatEventHelperService;
import com.fairing.fairplay.chat.service.ChatMessageService;
import com.fairing.fairplay.chat.service.ChatRoomService;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatEventHelperService chatEventHelperService;
    private final UserRepository userRepository;

    @Value("${llm.bot-user-id:999}")
    private Long botUserId;

    // [유저/관리자] 내 채팅방 리스트 (역할에 따라 자동으로 관리자 채팅방도 포함)
    @GetMapping("/rooms")
    public List<ChatRoomResponseDto> getMyChatRooms(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        String userRole = userDetails.getRoleCode();
        
        // 기본 사용자 채팅방
        List<ChatRoom> userRooms = chatRoomService.getRoomsByUser(userId);
        List<ChatRoomResponseDto> allRooms = new ArrayList<>();
        
        // 사용자가 참여한 채팅방 추가
        for (ChatRoom room : userRooms) {
            Long unreadCount = chatMessageService.countUnreadMessages(room, userId, userRole);
            allRooms.add(ChatRoomResponseDto.builder()
                    .chatRoomId(room.getChatRoomId())
                    .eventId(room.getEventId())
                    .userId(room.getUserId())
                    .targetType(room.getTargetType().name())
                    .targetId(room.getTargetId())
                    .createdAt(room.getCreatedAt())
                    .closedAt(room.getClosedAt())
                    .eventTitle(room.getEventId() != null ? 
                        chatEventHelperService.getEventTitle(room.getEventId()) : 
                        "전체 관리자 문의")
                    .unreadCount(unreadCount)
                    .build());
        }
        
        // 관리자인 경우 관리하는 채팅방도 추가 (백엔드에서 역할 체크)
        if ("ADMIN".equals(userRole)) {
            // 전체 관리자: 모든 ADMIN 타입 채팅방 (1:N 구조)
            List<ChatRoom> adminRooms = chatRoomService.getAllAdminRooms();
            addManagerRooms(allRooms, adminRooms, userId, userRole);
            
        } else if ("EVENT_MANAGER".equals(userRole)) {
            // 행사 담당자: 자신이 담당하는 EVENT_MANAGER 타입 채팅방
            List<ChatRoom> eventManagerRooms = chatRoomService.getRoomsByManager(TargetType.EVENT_MANAGER, userId);
            addManagerRooms(allRooms, eventManagerRooms, userId, userRole);
            
        } else if ("BOOTH_MANAGER".equals(userRole)) {
            // 부스 담당자: 자신이 담당하는 BOOTH_MANAGER 타입 채팅방
            List<ChatRoom> boothManagerRooms = chatRoomService.getRoomsByManager(TargetType.BOOTH_MANAGER, userId);
            addManagerRooms(allRooms, boothManagerRooms, userId, userRole);
        }
        
        return allRooms.stream()
                .distinct() // 중복 제거
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // 최신순 정렬
                .collect(Collectors.toList());
    }
    
    // 관리자 채팅방을 추가하는 헬퍼 메소드
    private void addManagerRooms(List<ChatRoomResponseDto> allRooms, List<ChatRoom> managerRooms, Long userId, String roleCode) {
        Set<Long> existingRoomIds = allRooms.stream()
                .map(ChatRoomResponseDto::getChatRoomId)
                .collect(Collectors.toSet());
        List<ChatRoom> roomsToAdd = managerRooms.stream()
                .filter(room -> !existingRoomIds.contains(room.getChatRoomId()))
                .toList();
        Map<Long, String> userNamesById = userRepository.findAllById(roomsToAdd.stream()
                        .map(ChatRoom::getUserId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(
                        Users::getUserId,
                        Users::getName,
                        (left, right) -> left));
        
        for (ChatRoom room : roomsToAdd) {
            // 관리자로서 읽지 않은 메시지 수 계산
            Long unreadCount = chatMessageService.countUnreadMessages(room, userId, roleCode);
            String userName = userNamesById.getOrDefault(room.getUserId(), "알 수 없는 사용자");

            allRooms.add(ChatRoomResponseDto.builder()
                    .chatRoomId(room.getChatRoomId())
                    .eventId(room.getEventId())
                    .userId(room.getUserId())
                    .targetType(room.getTargetType().name())
                    .targetId(room.getTargetId())
                    .createdAt(room.getCreatedAt())
                    .closedAt(room.getClosedAt())
                    .eventTitle(room.getEventId() != null ? 
                        chatEventHelperService.getEventTitle(room.getEventId()) : 
                        "전체 관리자 문의")
                    .userName(userName)
                    .unreadCount(unreadCount)
                    .build());
        }
    }

    // [관리자] 내가 관리하는 채팅방 리스트 (인증 없이 테스트)
    @GetMapping("/rooms/manager")
    public List<ChatRoomResponseDto> getChatRoomsByManager(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String targetType,
            @RequestParam Long targetId
    ) {
        if (!"ADMIN".equals(userDetails.getRoleCode()) && !userDetails.getUserId().equals(targetId)) {
            throw new AccessDeniedException("다른 관리자의 채팅방을 조회할 수 없습니다.");
        }

        System.out.println("=== 관리자 채팅방 조회 API 호출 ===");
        System.out.println("targetType: " + targetType);
        System.out.println("targetId: " + targetId);
        
        TargetType tType = TargetType.valueOf(targetType);
        List<ChatRoom> rooms = chatRoomService.getRoomsByManager(tType, targetId);
        
        System.out.println("조회된 채팅방 수: " + rooms.size());
        for (ChatRoom room : rooms) {
            System.out.println("Room ID: " + room.getChatRoomId() + 
                               ", User ID: " + room.getUserId() + 
                               ", Target ID: " + room.getTargetId() + 
                               ", Event ID: " + room.getEventId());
        }
        System.out.println("===========================");
        
        return rooms.stream()
                .map(room -> {
                    // 사용자 이름 가져오기
                    String userName = userRepository.findById(room.getUserId())
                        .map(user -> user.getName())
                        .orElse("알 수 없는 사용자");
                    
                    // 읽지 않은 메시지 수 계산
                    Long unreadCount = chatMessageService.countUnreadMessages(room, targetId, userDetails.getRoleCode());
                    
                    return ChatRoomResponseDto.builder()
                        .chatRoomId(room.getChatRoomId())
                        .eventId(room.getEventId())
                        .userId(room.getUserId())
                        .targetType(room.getTargetType().name())
                        .targetId(room.getTargetId())
                        .createdAt(room.getCreatedAt())
                        .closedAt(room.getClosedAt())
                        .eventTitle(room.getEventId() != null ? 
                            chatEventHelperService.getEventTitle(room.getEventId()) : 
                            "전체 관리자 문의")
                        .userName(userName)
                        .unreadCount(unreadCount)
                        .build();
                })
                .collect(Collectors.toList());
    }

    // 채팅방 생성/입장(없으면 생성, 있으면 반환)
    @PostMapping("/room")
    public ChatRoomResponseDto createOrEnterRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String targetType,
            @RequestParam Long targetId,
            @RequestParam(required = false) Long eventId
    ) {
        Long userId = userDetails.getUserId();
        TargetType tType = TargetType.valueOf(targetType);
        ChatRoom room = chatRoomService.getOrCreateRoom(userId, tType, targetId, eventId);
        return ChatRoomResponseDto.builder()
                .chatRoomId(room.getChatRoomId())
                .eventId(room.getEventId())
                .userId(room.getUserId())
                .targetType(room.getTargetType().name())
                .targetId(room.getTargetId())
                .createdAt(room.getCreatedAt())
                .closedAt(room.getClosedAt())
                .eventTitle(room.getEventId() != null ? 
                    chatEventHelperService.getEventTitle(room.getEventId()) : 
                    "전체 관리자 문의")
                .build();
    }

    // 채팅방 내 메시지 전체(시간순)
    @GetMapping("/messages")
    public List<ChatMessageResponseDto> getMessages(
            @RequestParam Long chatRoomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return chatMessageService.getMessages(chatRoomId, userDetails.getUserId(), userDetails.getRoleCode());
    }

    // 페이징된 메시지 조회 (페이지 기반)
    @GetMapping("/messages/paged")
    public ChatMessagePageResponseDto getMessagesPaged(
            @RequestParam Long chatRoomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return chatMessageService.getMessagesPaged(chatRoomId, userDetails.getUserId(), userDetails.getRoleCode(), page, size);
    }

    // 커서 기반 무한스크롤 메시지 조회
    @GetMapping("/messages/cursor")
    public ChatMessagePageResponseDto getMessagesWithCursor(
            @RequestParam Long chatRoomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return chatMessageService.getMessagesWithCursor(chatRoomId, userDetails.getUserId(), userDetails.getRoleCode(), lastMessageId, size);
    }

    // 메시지 전송
    @PostMapping("/message")
    public ChatMessageResponseDto sendMessage(
            @RequestBody ChatMessageRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long senderId = userDetails.getUserId();
        return chatMessageService.sendMessage(dto.getChatRoomId(), senderId, userDetails.getRoleCode(), dto.getContent());
    }

    // 안읽은 메시지 개수
    @GetMapping("/unread-count")
    public Long countUnreadMessages(
            @RequestParam Long chatRoomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long myUserId = userDetails.getUserId();
        return chatMessageService.countUnreadMessages(chatRoomId, myUserId, userDetails.getRoleCode());
    }

    // 채팅방의 모든 메시지를 읽음으로 처리
    @PatchMapping("/messages/read")
    public void markMessagesAsRead(
            @RequestParam Long chatRoomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long myUserId = userDetails.getUserId();
        chatMessageService.markRoomMessagesAsRead(chatRoomId, myUserId, userDetails.getRoleCode());
    }

    // 👉 이벤트 담당자 문의용 API
    @PostMapping("/event-inquiry")
    public ChatRoomResponseDto eventInquiry(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Long> body
    ) {
        Long userId = userDetails.getUserId();
        Long eventId = body.get("eventId");
        Long managerId = chatEventHelperService.getManagerUserId(eventId);

        ChatRoom room = chatRoomService.getOrCreateRoom(
                userId, TargetType.EVENT_MANAGER, managerId, eventId
        );

        return ChatRoomResponseDto.builder()
                .chatRoomId(room.getChatRoomId())
                .eventId(room.getEventId())
                .userId(room.getUserId())
                .targetType(room.getTargetType().name())
                .targetId(room.getTargetId())
                .createdAt(room.getCreatedAt())
                .closedAt(room.getClosedAt())
                .eventTitle(chatEventHelperService.getEventTitle(eventId))
                .build();
    }

    // 👉 전체 관리자 문의용 API (1:N 구조로 ADMIN 권한 사용자들이 모두 볼 수 있음)
    @PostMapping("/admin-inquiry")
    public ChatRoomResponseDto adminInquiry(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        
        // ADMIN 권한을 가진 사용자 ID를 찾아서 연결
        Long adminUserId = chatEventHelperService.getAdminUserId();
        
        ChatRoom room = chatRoomService.getOrCreateRoom(
                userId, TargetType.ADMIN, adminUserId, null
        );

        return ChatRoomResponseDto.builder()
                .chatRoomId(room.getChatRoomId())
                .eventId(room.getEventId())
                .userId(room.getUserId())
                .targetType(room.getTargetType().name())
                .targetId(room.getTargetId())
                .createdAt(room.getCreatedAt())
                .closedAt(room.getClosedAt())
                .eventTitle("전체 관리자 문의")
                .build();
    }

    @PostMapping("/ai-inquiry")
    public ChatRoomResponseDto aiInquiry(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();

        // 봇 계정 ID 사용

        ChatRoom room = chatRoomService.getOrCreateRoom(
                userId,
                TargetType.AI, // 👈 AI 방 타입
                botUserId,
                null // eventId 없음
        );

        return ChatRoomResponseDto.builder()
                .chatRoomId(room.getChatRoomId())
                .eventId(room.getEventId())
                .userId(room.getUserId())
                .targetType(room.getTargetType().name())
                .targetId(room.getTargetId())
                .createdAt(room.getCreatedAt())
                .closedAt(room.getClosedAt())
                .eventTitle("AI 도우미") // 표시용
                .build();
    }



}
