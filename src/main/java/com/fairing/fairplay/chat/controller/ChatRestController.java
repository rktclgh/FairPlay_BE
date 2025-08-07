package com.fairing.fairplay.chat.controller;

import com.fairing.fairplay.chat.dto.ChatMessageRequestDto;
import com.fairing.fairplay.chat.dto.ChatMessageResponseDto;
import com.fairing.fairplay.chat.dto.ChatRoomResponseDto;
import com.fairing.fairplay.chat.entity.ChatRoom;
import com.fairing.fairplay.chat.entity.TargetType;
import com.fairing.fairplay.chat.repository.ChatRoomRepository;
import com.fairing.fairplay.chat.service.ChatEventHelperService;
import com.fairing.fairplay.chat.service.ChatMessageService;
import com.fairing.fairplay.chat.service.ChatRoomService;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
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
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

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
            Long unreadCount = chatMessageService.countUnreadMessages(room.getChatRoomId(), userId);
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
            // 전체 관리자: 모든 ADMIN 타입 채팅방
            List<ChatRoom> adminRooms = chatRoomService.getRoomsByManager(TargetType.ADMIN, userId);
            addManagerRooms(allRooms, adminRooms, userId);
            
        } else if ("EVENT_MANAGER".equals(userRole)) {
            // 행사 담당자: 자신이 담당하는 EVENT_MANAGER 타입 채팅방
            List<ChatRoom> eventManagerRooms = chatRoomService.getRoomsByManager(TargetType.EVENT_MANAGER, userId);
            addManagerRooms(allRooms, eventManagerRooms, userId);
            
        } else if ("BOOTH_MANAGER".equals(userRole)) {
            // 부스 담당자: 자신이 담당하는 BOOTH_MANAGER 타입 채팅방
            List<ChatRoom> boothManagerRooms = chatRoomService.getRoomsByManager(TargetType.BOOTH_MANAGER, userId);
            addManagerRooms(allRooms, boothManagerRooms, userId);
        }
        
        return allRooms.stream()
                .distinct() // 중복 제거
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // 최신순 정렬
                .collect(Collectors.toList());
    }
    
    // 관리자 채팅방을 추가하는 헬퍼 메소드
    private void addManagerRooms(List<ChatRoomResponseDto> allRooms, List<ChatRoom> managerRooms, Long userId) {
        Set<Long> existingRoomIds = allRooms.stream()
                .map(ChatRoomResponseDto::getChatRoomId)
                .collect(Collectors.toSet());
        
        for (ChatRoom room : managerRooms) {
            if (!existingRoomIds.contains(room.getChatRoomId())) {
                // 관리자로서 읽지 않은 메시지 수 계산
                Long unreadCount = chatMessageService.countUnreadMessages(room.getChatRoomId(), userId);
                
                // 사용자 이름 가져오기
                String userName = userRepository.findById(room.getUserId())
                    .map(user -> user.getName())
                    .orElse("알 수 없는 사용자");
                
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
    }

    // [관리자] 내가 관리하는 채팅방 리스트 (인증 없이 테스트)
    @GetMapping("/rooms/manager")
    public List<ChatRoomResponseDto> getChatRoomsByManager(
            @RequestParam String targetType,
            @RequestParam Long targetId
    ) {
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
                    Long unreadCount = chatMessageService.countUnreadMessages(room.getChatRoomId(), targetId);
                    
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
    public List<ChatMessageResponseDto> getMessages(@RequestParam Long chatRoomId) {
        return chatMessageService.getMessages(chatRoomId);
    }

    // 메시지 전송
    @PostMapping("/message")
    public ChatMessageResponseDto sendMessage(
            @RequestBody ChatMessageRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long senderId = userDetails.getUserId();
        return chatMessageService.sendMessage(dto.getChatRoomId(), senderId, dto.getContent());
    }

    // 안읽은 메시지 개수
    @GetMapping("/unread-count")
    public Long countUnreadMessages(
            @RequestParam Long chatRoomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long myUserId = userDetails.getUserId();
        return chatMessageService.countUnreadMessages(chatRoomId, myUserId);
    }

    // 채팅방의 모든 메시지를 읽음으로 처리
    @PatchMapping("/messages/read")
    public void markMessagesAsRead(
            @RequestParam Long chatRoomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long myUserId = userDetails.getUserId();
        chatMessageService.markRoomMessagesAsRead(chatRoomId, myUserId);
    }


    // 디버깅용: 모든 채팅방 조회
    @GetMapping("/debug/all-rooms")
    public List<ChatRoomResponseDto> getAllRoomsForDebug() {
        List<ChatRoom> allRooms = chatRoomRepository.findAll();
        System.out.println("=== 전체 채팅방 디버깅 ===");
        System.out.println("총 채팅방 수: " + allRooms.size());
        
        for (ChatRoom room : allRooms) {
            System.out.println("Room ID: " + room.getChatRoomId() + 
                               ", User ID: " + room.getUserId() + 
                               ", Target Type: " + room.getTargetType() + 
                               ", Target ID: " + room.getTargetId() + 
                               ", Event ID: " + room.getEventId() +
                               ", Created: " + room.getCreatedAt());
        }
        System.out.println("=======================");
        
        return allRooms.stream()
                .map(room -> ChatRoomResponseDto.builder()
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
                        .build())
                .collect(Collectors.toList());
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
}
