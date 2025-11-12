# FairPlay Backend 🎪

<div align="center">

**종합 행사 예약 및 관리 플랫폼 - 백엔드 서버**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-blue.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-6.x-red.svg)](https://redis.io/)

[**프로젝트 홈페이지**](https://fair-play.ink) | [**API 문서**](https://fair-play.ink/swagger-ui.html) | [**GitHub**](https://github.com/rktclgh/FairPlay_BE)

</div>

---

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [주요 기능](#-주요-기능)
  - [RAG 기반 AI 챗봇](#1-️-rag-기반-ai-챗봇)
  - [실시간 채팅](#2--실시간-채팅-시스템)
  - [멀티테넌트 권한 관리](#3--멀티테넌트-권한-관리-시스템)
  - [QR 입퇴장 관리](#4--qr-코드-기반-실시간-입퇴장-관리)
- [기술 스택](#️-기술-스택)
- [시스템 아키텍처](#-시스템-아키텍처)
- [설치 및 실행](#-설치-및-실행)
- [API 문서](#-api-문서)
- [데이터베이스 구조](#️-데이터베이스-구조)
- [배포](#-배포)

---

## 🎯 프로젝트 개요

**FairPlay**는 전시회, 페스티벌, 부스 운영 등 다양한 행사를 효율적으로 관리하고 예약할 수 있는 종합 이벤트 관리 플랫폼의 백엔드 서버입니다.

### 💡 개발 배경

- 행사 주최자와 참가자 간의 원활한 소통 지원 필요
- 부스 운영자의 효율적인 관리 도구 제공
- 실시간 입퇴장 관리 및 통계 분석 제공
- AI 기반 자동 고객 지원 시스템 구축

### 📅 개발 정보

- **개발 기간**: 2025.07 - 2025.08 (2개월)
- **팀 구성**: 백엔드 5명, 프론트엔드 2명
- **배포 환경**: AWS EC2, RDS, S3, CloudFront CDN
- **프로덕션 URL**: https://fair-play.ink

---

## ✨ 주요 기능

### 1. 🤖 RAG 기반 AI 챗봇

> **Retrieval Augmented Generation을 활용한 지능형 고객 지원 시스템**

<!-- 여기에 AI 챗봇 인터페이스 스크린샷 추가 -->

**핵심 기술 스택**
- **LLM 모델**: Google Gemini 2.5-flash
- **벡터 임베딩**: Google Embedding API
- **벡터 스토어**: Redis (의미 기반 검색)
- **실시간 응답**: WebSocket 기반 스트리밍

**주요 기능**
```
✅ 행사 정보 자동 임베딩 및 인덱싱
✅ 사용자 질의에 대한 의미 기반 검색 (Semantic Search)
✅ 컨텍스트 기반 정확한 답변 생성
✅ 대화 히스토리 관리 및 문맥 유지
✅ 관리자용 문서 업로드 및 관리 인터페이스
✅ 실시간 스트리밍 응답 (WebSocket)
```

**RAG 파이프라인**

```
사용자 질의 입력
    ↓
[1. 문서 전처리]
    - ChunkingService: 문서를 의미 단위로 분할
    - 최적 청크 크기 설정 (512 tokens)
    ↓
[2. 벡터 임베딩]
    - EmbeddingService: Google Embedding API 호출
    - 텍스트 → 벡터 변환 (768차원)
    ↓
[3. 벡터 저장]
    - RagRedisRepository: Redis에 벡터 저장
    - 메타데이터 함께 저장 (source, event_id 등)
    ↓
[4. 의미 기반 검색]
    - 사용자 질의 벡터화
    - VectorSearchService: 유사도 계산 (Cosine Similarity)
    - Top-K 관련 문서 추출 (K=5)
    ↓
[5. LLM 응답 생성]
    - RagChatService: 검색된 문서 + 질의 → Prompt 구성
    - Google Gemini API 호출
    - 컨텍스트 기반 답변 생성
    ↓
[6. 실시간 응답 전송]
    - WebSocket으로 스트리밍 전송
    - 사용자에게 실시간 답변 표시
```

**구현 상세**

```java
// 주요 서비스 클래스
- ChunkingService: 문서 분할 및 전처리
- EmbeddingService: 벡터 임베딩 생성
- VectorSearchService: 의미 기반 검색
- RagChatService: RAG + LLM 통합 오케스트레이션
- RagRedisRepository: Redis 벡터 스토어 관리
- DocumentIngestService: 문서 업로드 및 인덱싱

// 데이터 로더
- EventRagDataLoader: 행사 데이터 자동 임베딩
- ComprehensiveRagDataLoader: 시스템 전체 데이터 로딩
```

**API 엔드포인트**

```http
POST   /api/ai/chat                      # AI 챗봇과 대화 (WebSocket/REST)
POST   /api/admin/rag/ingest             # 문서 임베딩 및 저장 (관리자)
GET    /api/admin/rag/documents          # 임베딩된 문서 목록 조회
DELETE /api/admin/rag/documents/{id}     # 문서 삭제
GET    /api/admin/rag/stats              # RAG 시스템 통계
```

**사용 예시**

```
사용자: "송도 맥주축제는 언제 열리나요?"
    ↓
벡터 검색: "송도 맥주축제" 관련 문서 5개 추출
    ↓
Gemini API: 컨텍스트 기반 답변 생성
    ↓
응답: "송도 맥주축제는 2024년 8월 15일부터 8월 18일까지
      송도 센트럴파크에서 개최됩니다. 다양한 수제 맥주와
      푸드 트럭이 준비되어 있으며, 티켓 예매는 현재
      진행 중입니다."
```

---

### 2. 💬 실시간 채팅 시스템

> **WebSocket 기반 실시간 메시징 플랫폼**

<!-- 여기에 채팅 인터페이스 스크린샷 추가 -->

**기술 구현**
- **프로토콜**: Spring WebSocket + STOMP
- **폴백**: SockJS (WebSocket 미지원 브라우저 대응)
- **메시지 브로커**: Redis Pub/Sub (분산 환경 지원)
- **인증**: JWT 토큰 기반 WebSocket 핸드셰이크

**주요 기능**

```
✅ 1:1 및 그룹 채팅 지원
✅ 실시간 메시지 전송 및 수신
✅ 사용자 온라인/오프라인 상태 추적
✅ 메시지 영구 저장 (MySQL)
✅ 읽지 않은 메시지 카운트
✅ 파일 첨부 지원 (이미지, 문서)
✅ 메시지 검색 기능
✅ 자동 재연결 및 세션 복구
✅ 타이핑 인디케이터
```

**WebSocket 연결 플로우**

```
클라이언트 연결 요청
    ↓
[1. JWT 검증]
    - JwtHandshakeInterceptor
    - Access Token 검증
    - 실패 시 연결 거부
    ↓
[2. WebSocket 연결 수립]
    - STOMP over SockJS
    - 하트비트 설정 (25초)
    ↓
[3. 채널 구독]
    - ChannelInterceptor: 구독 권한 검증
    - /topic/chat/{roomId}: 채팅방 구독
    - /user/{userId}/queue/notifications: 개인 알림
    ↓
[4. 메시지 송수신]
    - SEND /app/chat/send
    - SUBSCRIBE /topic/chat/{roomId}
    ↓
[5. 세션 관리]
    - PresenceScheduler: 30초마다 비활성 세션 정리
    - Redis에 사용자 온라인 상태 저장
```

**WebSocket 설정**

```yaml
websocket:
  endpoint: /ws/chat
  heartbeat:
    client: 25000ms      # 클라이언트 하트비트
    server: 25000ms      # 서버 하트비트
  disconnect-delay: 30000ms
  message-size-limit: 64KB
```

**STOMP 엔드포인트**

```
WS     /ws/chat                              # WebSocket 연결
POST   /app/chat/send                        # 메시지 전송
SUB    /topic/chat/{roomId}                  # 채팅방 구독
SUB    /user/{userId}/queue/notifications    # 개인 알림 구독
```

**구현 상세**

```java
// WebSocket 설정
- WebSocketConfig: STOMP 및 메시지 브로커 설정
- JwtHandshakeInterceptor: 연결 시 JWT 검증
- ChannelInterceptor: 구독 권한 검증

// 서비스 계층
- ChatMessageService: 메시지 CRUD 및 읽음 처리
- ChatRoomService: 채팅방 생성 및 관리
- PresenceService: 사용자 온라인 상태 관리

// 스케줄러
- PresenceScheduler: 30초마다 비활성 세션 정리
- MessageSyncScheduler: 메시지 동기화 (분산 환경)
```

---

### 3. 🔐 멀티테넌트 권한 관리 시스템

> **계층적 역할 기반 접근 제어 (RBAC) 및 데이터 격리**

<!-- 여기에 권한 관리 시스템 다이어그램 추가 -->

#### 역할 계층 구조

```
┌─────────────────────────────────────────────┐
│           ADMIN (전체 관리자)                │
│  🔑 플랫폼 전체 관리 권한                     │
│  • 모든 시스템 기능 접근                      │
│  • 사용자 역할 부여/박탈                      │
│  • 플랫폼 전체 통계 및 KPI 조회               │
│  • 결제 및 정산 관리                          │
│  • VIP 배너 광고 승인                         │
└──────────────┬──────────────────────────────┘
               │
               ├──────────────────────────────────┐
               │                                  │
     ┌─────────▼──────────┐           ┌──────────▼─────────┐
     │  EVENT_ADMIN       │           │  BOOTH_ADMIN       │
     │  (행사 관리자)      │           │  (부스 관리자)      │
     │  🎪 행사 운영 권한  │           │  🏪 부스 운영 권한  │
     │  • 행사 생성/수정   │           │  • 부스 신청/관리   │
     │  • 예약 관리       │           │  • QR 스캔 권한     │
     │  • 티켓 발급       │           │  • 체험 프로그램    │
     │  • 정산 요청       │           │  • 방문자 통계      │
     │  • 부스 승인       │           │                    │
     └────────┬───────────┘           └────────────────────┘
              │
     ┌────────▼──────────┐
     │      USER         │
     │  (일반 사용자)     │
     │  👤 기본 권한      │
     │  • 행사 예약      │
     │  • 리뷰 작성      │
     │  • 위시리스트     │
     │  • 명함 교환      │
     └───────────────────┘
```

#### 주요 권한 기능

**1️⃣ 전체 관리자 (ADMIN) 기능**

<!-- 여기에 관리자 대시보드 스크린샷 추가 -->

```
✅ 사용자 권한 관리
   - EVENT_ADMIN 권한 부여/박탈
   - BOOTH_ADMIN 권한 부여/박탈
   - 사용자 역할 히스토리 조회

✅ 행사 관리
   - 모든 행사 조회 및 수정
   - 행사 승인/거부
   - 행사 버전 관리

✅ 결제 및 정산
   - 전체 결제 내역 조회
   - 정산 승인 및 처리
   - 환불 관리

✅ 플랫폼 통계
   - 실시간 KPI 대시보드
   - 사용자/행사/매출 통계
   - 이상 거래 탐지

✅ VIP 배너 관리
   - 광고 배너 승인/거부
   - 배너 노출 순위 관리

✅ 시스템 관리
   - 보안 로그 조회
   - 변경 이력 추적
   - 이메일 템플릿 관리
```

**관리자 API**

```http
# 권한 관리
POST   /api/admin/grant-event-admin/{userId}      # 행사 관리자 권한 부여
DELETE /api/admin/revoke-event-admin/{userId}     # 행사 관리자 권한 박탈
POST   /api/admin/grant-booth-admin/{userId}      # 부스 관리자 권한 부여
DELETE /api/admin/revoke-booth-admin/{userId}     # 부스 관리자 권한 박탈
GET    /api/admin/users                           # 전체 사용자 관리
GET    /api/admin/role-history/{userId}           # 권한 변경 이력

# 대시보드
GET    /api/admin/dashboard                       # 관리자 대시보드
GET    /api/admin/statistics/kpi                  # KPI 통계
GET    /api/admin/statistics/revenue              # 매출 통계

# 행사 관리
GET    /api/admin/events                          # 전체 행사 조회
PUT    /api/admin/events/{eventId}/approve        # 행사 승인
PUT    /api/admin/events/{eventId}/reject         # 행사 거부

# 결제 및 정산
GET    /api/admin/payments                        # 전체 결제 내역
POST   /api/admin/settlements/{id}/approve        # 정산 승인
GET    /api/admin/refunds                         # 환불 요청 관리
```

**2️⃣ 행사 관리자 (EVENT_ADMIN) 기능**

<!-- 여기에 행사 관리자 대시보드 스크린샷 추가 -->

```
✅ 행사 운영
   - 행사 생성 및 수정
   - 티켓 종류 및 가격 설정
   - 일정 및 세션 관리
   - 행사 버전 관리 (수정 이력)

✅ 예약 관리
   - 예약 현황 실시간 조회
   - 참가자 명단 다운로드 (Excel)
   - 예약 취소 및 환불 처리
   - 대기열 관리

✅ 부스 관리
   - 부스 신청 승인/거부
   - 부스 배치 및 위치 관리
   - 부스 관리자 권한 부여

✅ QR 티켓 관리
   - QR 티켓 일괄 발급
   - 입퇴장 로그 조회
   - 실시간 입장 현황

✅ 통계 및 분석
   - 행사별 매출 분석
   - 예약 통계 (시간대별, 티켓별)
   - 참가자 인구 통계
   - 리뷰 및 만족도 분석

✅ 정산
   - 정산 요청
   - 정산 내역 조회
   - 수익금 확인
```

**3️⃣ 부스 관리자 (BOOTH_ADMIN) 기능**

<!-- 여기에 부스 관리자 QR 스캔 화면 스크린샷 추가 -->

```
✅ 부스 운영
   - 부스 정보 수정
   - 운영 시간 관리
   - 부스 이미지 및 설명 업데이트

✅ QR 스캔 모드
   - 실시간 QR 코드 스캔
   - 입장 권한 검증
   - 체험 프로그램 참여 기록

✅ 체험 관리
   - 체험 프로그램 생성
   - 참여자 관리
   - 체험 후기 조회

✅ 통계
   - 방문자 수 통계
   - 체험 참여율
   - 인기 시간대 분석
```

**4️⃣ 일반 사용자 (USER) 기능**

```
✅ 행사 탐색
   - 행사 검색 및 필터링
   - 카테고리별 행사 보기
   - 인기 행사 추천

✅ 예약 및 티켓
   - 행사 예약
   - QR 티켓 발급
   - 예약 내역 조회
   - 예약 취소

✅ 소셜 기능
   - 리뷰 및 평점 작성
   - 위시리스트 관리
   - 디지털 명함 생성 및 교환

✅ 마이페이지
   - 프로필 관리
   - 예약 내역
   - 리뷰 관리
   - 명함 지갑
```

#### 멀티테넌트 데이터 격리

**Event Namespace (행사별 데이터 격리)**

```java
// 행사 관리자는 자신이 생성한 행사만 관리 가능
@PreAuthorize("hasRole('EVENT_ADMIN')")
public List<Event> getMyEvents(Long eventAdminId) {
    return eventRepository.findByCreatorId(eventAdminId);
}

// 다른 관리자의 행사는 조회 불가
@PreAuthorize("hasRole('EVENT_ADMIN')")
public Event getEvent(Long eventId, Long eventAdminId) {
    Event event = eventRepository.findById(eventId)
        .orElseThrow(() -> new EventNotFoundException());

    if (!event.getCreatorId().equals(eventAdminId)) {
        throw new UnauthorizedException();
    }

    return event;
}
```

**Booth Namespace (부스별 데이터 격리)**

```java
// 부스 관리자는 자신의 부스만 접근 가능
@PreAuthorize("hasRole('BOOTH_ADMIN')")
public Booth getMyBooth(Long boothAdminId) {
    return boothRepository.findByAdminId(boothAdminId)
        .orElseThrow(() -> new BoothNotFoundException());
}
```

#### 권한 검증 메커니즘

**메서드 레벨 보안 (Custom Annotation)**

```java
// 커스텀 어노테이션 정의
@FunctionAuth(roles = {UserRole.ADMIN})
public void deleteUser(Long userId) {
    userRepository.deleteById(userId);
}

@FunctionAuth(roles = {UserRole.EVENT_ADMIN, UserRole.ADMIN})
public Event createEvent(EventDto dto) {
    // 행사 생성 로직
}
```

**Spring Security 설정**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/host/**").hasAnyRole("EVENT_ADMIN", "ADMIN")
            .requestMatchers("/api/booth-admin/**").hasAnyRole("BOOTH_ADMIN", "ADMIN")
            .requestMatchers("/api/public/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

**JWT 기반 인증**

```java
// JWT 토큰 생성
public String generateAccessToken(UserDetails userDetails) {
    return Jwts.builder()
        .setSubject(userDetails.getUsername())
        .claim("roles", userDetails.getAuthorities())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
        .signWith(SignatureAlgorithm.HS512, JWT_SECRET)
        .compact();
}
```

---

### 4. 📱 QR 코드 기반 실시간 입퇴장 관리

> **QR 티켓 생성, 검증 및 실시간 입퇴장 추적 시스템**

<!-- 여기에 QR 티켓 및 스캔 화면 스크린샷 추가 -->

#### QR 티켓 시스템 개요

FairPlay의 QR 시스템은 행사 예약 시 자동으로 고유 QR 코드를 생성하고, 이를 통해 빠르고 안전한 입퇴장 관리를 제공합니다.

**핵심 기능**

```
✅ 예약 시 자동 QR 티켓 생성
✅ QR 코드를 통한 실시간 입장/퇴장 검증
✅ 중복 입장 방지 (티켓 상태 관리)
✅ 입장 시간대 및 정책 검증
✅ 분실 시 티켓 재발급
✅ 게스트 티켓 지원
✅ 전체 입퇴장 이력 로그 및 감사
✅ 실시간 통계 업데이트
```

#### QR 티켓 생성 및 검증 플로우

```
┌──────────────────┐
│  1. 행사 예약     │
│  - 사용자가 티켓  │
│    선택 및 결제   │
└────────┬─────────┘
         │
         ▼
┌─────────────────────────────┐
│  2. QR 티켓 자동 생성        │
│  - 고유 해시 생성            │
│    (SHA-256 + Salt)          │
│  - 티켓 ID 암호화            │
│    (Hashids)                 │
│  - 유효기간 설정             │
│  - DB 저장                   │
└────────┬────────────────────┘
         │
         ▼
┌────────────────────────────┐
│  3. 사용자에게 QR 전달      │
│  - 모바일 티켓 표시         │
│  - 이메일로 QR 전송         │
│  - PDF 다운로드 옵션        │
└────────┬───────────────────┘
         │
         ▼
┌────────────────────────────┐
│  4. 입장 시 QR 스캔         │
│  - 스마트폰 카메라로 인식   │
│  - 또는 전용 스캐너 사용    │
│  - 서버로 검증 요청         │
└────────┬───────────────────┘
         │
         ▼
┌──────────────────────────────┐
│  5. 서버 검증 프로세스        │
│  ✓ 티켓 디코딩 및 조회        │
│  ✓ 티켓 상태 확인            │
│    (VALID/USED/EXPIRED)      │
│  ✓ 중복 사용 확인            │
│  ✓ 입장 시간대 확인          │
│  ✓ 인원 제한 확인            │
│  ✓ 사용자 권한 확인          │
└────────┬─────────────────────┘
         │
         ▼
┌──────────────────────────┐
│  6. 입장 승인/거부        │
│  - QR 로그 기록          │
│  - 티켓 상태 업데이트    │
│  - 통계 실시간 갱신      │
│  - 알림 전송             │
└──────────────────────────┘
```

#### QR 티켓 검증 로직 상세

**1단계: 티켓 디코딩**

```java
@Service
public class QrTicketVerificationService {

    public QrVerificationResult verifyTicket(String qrCodeData) {
        // 1. QR 코드에서 티켓 해시 추출
        String ticketHash = qrCodeDecoder.decode(qrCodeData);

        // 2. DB에서 티켓 조회
        QrTicket ticket = qrTicketRepository.findByHash(ticketHash)
            .orElseThrow(() -> new TicketNotFoundException("티켓을 찾을 수 없습니다"));

        return performVerification(ticket);
    }
}
```

**2단계: 티켓 상태 검증**

```java
private void validateTicketStatus(QrTicket ticket) {
    // 이미 사용된 티켓 확인
    if (ticket.getStatus() == TicketStatus.USED) {
        throw new TicketAlreadyUsedException(
            "이미 사용된 티켓입니다. 사용 시간: " + ticket.getUsedAt()
        );
    }

    // 만료 확인
    if (ticket.isExpired()) {
        throw new TicketExpiredException(
            "만료된 티켓입니다. 유효기간: " + ticket.getExpiresAt()
        );
    }

    // 취소된 티켓 확인
    if (ticket.getStatus() == TicketStatus.CANCELLED) {
        throw new TicketCancelledException("취소된 티켓입니다");
    }
}
```

**3단계: 입장 정책 검증**

```java
@Service
public class QrEntryValidateService {

    public void validateEntryPolicy(QrTicket ticket, Event event) {
        LocalDateTime now = LocalDateTime.now();

        // 입장 시간대 확인
        if (now.isBefore(event.getEntryStartTime())) {
            throw new TooEarlyEntryException(
                "입장 시작 시간 전입니다. 입장 시작: " + event.getEntryStartTime()
            );
        }

        if (now.isAfter(event.getEntryEndTime())) {
            throw new EntryTimeExpiredException(
                "입장 마감되었습니다. 입장 마감: " + event.getEntryEndTime()
            );
        }

        // 인원 제한 확인
        long currentAttendance = qrCheckLogRepository
            .countCurrentAttendees(event.getId());

        if (currentAttendance >= event.getMaxCapacity()) {
            throw new CapacityExceededException(
                "행사장이 만석입니다. 최대 인원: " + event.getMaxCapacity()
            );
        }
    }
}
```

**4단계: 입장 처리 및 로깅**

```java
@Service
@Transactional
public class EntryExitService {

    public CheckInResult processCheckIn(QrTicket ticket, String location) {
        // 입장 기록 생성
        QrCheckLog log = QrCheckLog.builder()
            .ticket(ticket)
            .checkType(CheckType.CHECK_IN)
            .checkTime(LocalDateTime.now())
            .location(location)
            .scannedBy(SecurityContextHolder.getContext().getAuthentication().getName())
            .build();

        qrCheckLogRepository.save(log);

        // 티켓 상태 업데이트
        ticket.markAsUsed();
        ticket.setUsedAt(LocalDateTime.now());
        qrTicketRepository.save(ticket);

        // 실시간 통계 업데이트
        statisticsService.incrementAttendance(ticket.getEventId());

        // 알림 전송
        notificationService.sendCheckInNotification(ticket.getUserId());

        return CheckInResult.success(ticket);
    }
}
```

#### QR 티켓 보안 기능

**암호화 및 해싱**

```java
@Component
public class QrCodeGenerator {

    private final Hashids hashids;

    public String generateTicketHash(QrTicket ticket) {
        // 1. Hashids를 사용한 티켓 ID 암호화
        String encodedId = hashids.encode(ticket.getId());

        // 2. 추가 정보와 함께 SHA-256 해싱
        String payload = encodedId + "|" +
                        ticket.getUserId() + "|" +
                        ticket.getEventId() + "|" +
                        ticket.getIssuedAt().toString() + "|" +
                        generateRandomSalt();

        return DigestUtils.sha256Hex(payload);
    }

    private String generateRandomSalt() {
        return UUID.randomUUID().toString();
    }
}
```

**위조 방지 메커니즘**

```
✅ QR 코드에 암호화된 페이로드 포함
✅ 서버측 검증을 통한 이중 확인
✅ 일회용 토큰 (사용 후 즉시 무효화)
✅ 시간 기반 만료 (유효기간 설정)
✅ IP 기반 스캔 위치 추적
✅ 비정상 스캔 패턴 탐지
```

#### 주요 엔티티 구조

**QrTicket (QR 티켓)**

```java
@Entity
@Table(name = "qr_ticket")
public class QrTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String ticketHash;          // QR 코드 해시값

    private Long reservationId;         // 예약 ID
    private Long userId;                // 사용자 ID
    private Long eventId;               // 행사 ID

    @Enumerated(EnumType.STRING)
    private TicketStatus status;        // VALID, USED, EXPIRED, CANCELLED

    private LocalDateTime issuedAt;     // 발급 시간
    private LocalDateTime expiresAt;    // 만료 시간
    private LocalDateTime usedAt;       // 사용 시간

    private String scanLocation;        // 스캔 위치
    private String deviceInfo;          // 스캔 디바이스 정보
}
```

**QrCheckLog (입퇴장 로그)**

```java
@Entity
@Table(name = "qr_check_log")
public class QrCheckLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ticketId;              // 티켓 ID

    @Enumerated(EnumType.STRING)
    private CheckType checkType;        // CHECK_IN, CHECK_OUT

    private LocalDateTime checkTime;    // 검증 시간
    private String location;            // 스캔 위치
    private Long scannedBy;             // 스캔한 관리자 ID

    private String deviceInfo;          // 스캔 디바이스 정보
    private String ipAddress;           // IP 주소
}
```

#### API 엔드포인트

**티켓 발급 및 조회**

```http
GET    /api/qr/tickets/{ticketId}              # QR 티켓 조회
POST   /api/qr/reissue                         # 티켓 재발급
POST   /api/qr/batch-generate                  # 대량 티켓 생성
GET    /api/qr/tickets/user/{userId}           # 사용자 티켓 목록
```

**티켓 검증 및 입퇴장**

```http
POST   /api/qr/check                           # QR 코드 검증
POST   /api/qr/check-in                        # 입장 처리
POST   /api/qr/check-out                       # 퇴장 처리
GET    /api/qr/check-logs/{eventId}            # 입퇴장 이력 조회
GET    /api/qr/current-attendance/{eventId}    # 현재 입장 인원
```

**게스트 티켓**

```http
POST   /api/qr/guest-ticket                    # 게스트 티켓 생성
GET    /api/qr/guest-ticket/{guestToken}       # 게스트 티켓 조회
DELETE /api/qr/guest-ticket/{guestToken}       # 게스트 티켓 삭제
```

#### 실시간 입퇴장 통계

<!-- 여기에 실시간 통계 대시보드 스크린샷 추가 -->

**통계 집계 기능**

```
📊 실시간 현황
- 현재 입장자 수: 실시간 추적
- 금일 총 입장: 누적 집계
- 퇴장 인원: 체크아웃 기록
- 피크 타임: 시간대별 최대 입장자

📈 분석 지표
- 시간대별 입퇴장 패턴
- 평균 체류 시간
- 재입장 횟수
- 입장 경로 분석
```

**통계 API**

```http
GET /api/statistics/attendance/{eventId}       # 입장 통계
GET /api/statistics/hourly/{eventId}           # 시간대별 통계
GET /api/statistics/peak-time/{eventId}        # 피크 타임 조회
GET /api/statistics/average-stay/{eventId}     # 평균 체류 시간
```

---

## 🛠️ 기술 스택

### Backend Framework

| 기술 | 버전 | 용도 |
|-----|------|------|
| ![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk) | 21 (LTS) | 주 프로그래밍 언어 |
| ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot) | 3.2.5 | 애플리케이션 프레임워크 |
| ![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-green?logo=springsecurity) | 6.x | 인증 및 인가 |
| ![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-3.x-green) | 3.x | ORM 및 데이터 접근 |
| ![QueryDSL](https://img.shields.io/badge/QueryDSL-5.0.0-blue) | 5.0.0 | 타입 안전 쿼리 DSL |

### Database & Cache

| 기술 | 버전 | 용도 |
|-----|------|------|
| ![MySQL](https://img.shields.io/badge/MySQL-8.x-blue?logo=mysql) | 8.x | 관계형 데이터베이스 |
| ![Redis](https://img.shields.io/badge/Redis-6.x-red?logo=redis) | 6.x | 캐싱, 세션, RAG 벡터 스토어 |

### Real-time Communication

| 기술 | 버전 | 용도 |
|-----|------|------|
| ![Spring WebSocket](https://img.shields.io/badge/Spring%20WebSocket-6.x-green) | 6.x | 실시간 양방향 통신 |
| ![STOMP](https://img.shields.io/badge/STOMP-1.2-orange) | 1.2 | 메시징 프로토콜 |
| ![SockJS](https://img.shields.io/badge/SockJS-1.6-yellow) | 1.6 | WebSocket 폴백 |

### AI & Machine Learning

| 기술 | 버전 | 용도 |
|-----|------|------|
| ![Google Gemini](https://img.shields.io/badge/Google%20Gemini-2.5--flash-blue) | 2.5-flash | 대규모 언어 모델 (LLM) |
| ![Embedding API](https://img.shields.io/badge/Embedding-Gemini-blue) | - | 텍스트 벡터 임베딩 |
| **RAG** | Custom | 문서 검색 증강 생성 |

### Cloud & Storage

| 기술 | 용도 |
|-----|------|
| ![AWS S3](https://img.shields.io/badge/AWS%20S3-569A31?logo=amazons3) | 파일 저장소 (이미지, 문서) |
| ![CloudFront](https://img.shields.io/badge/CloudFront-CDN-orange) | 콘텐츠 전송 네트워크 |
| ![AWS RDS](https://img.shields.io/badge/AWS%20RDS-MySQL-blue) | 관리형 MySQL 데이터베이스 |
| ![AWS EC2](https://img.shields.io/badge/AWS%20EC2-Ubuntu-orange) | 애플리케이션 서버 |

### Build & Deployment

| 기술 | 버전 | 용도 |
|-----|------|------|
| ![Gradle](https://img.shields.io/badge/Gradle-8.x-green?logo=gradle) | 8.x | 빌드 자동화 도구 |
| ![Docker](https://img.shields.io/badge/Docker-24.x-blue?logo=docker) | 24.x | 컨테이너화 |
| ![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-CI/CD-black?logo=githubactions) | - | 지속적 통합 및 배포 |

### External APIs & Services

| 서비스 | 용도 |
|--------|------|
| **Iamport (아임포트)** | 결제 처리 (카드, 계좌이체 등) |
| **Kakao OAuth** | 소셜 로그인 |
| **Gmail SMTP** | 이메일 발송 (예약 확인, 알림) |
| **Google Gemini API** | AI 챗봇 및 RAG |

### Libraries & Utilities

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| **Lombok** | 1.18.x | 보일러플레이트 코드 제거 |
| **Apache POI** | 5.2.4 | Excel 파일 처리 (통계 다운로드) |
| **Hashids** | 1.0.3 | ID 암호화 및 인코딩 |
| **JJWT** | 0.9.1 | JWT 토큰 생성 및 검증 |
| **SpringDoc OpenAPI** | 2.x | API 문서 자동 생성 (Swagger) |
| **Jackson** | 2.19.0 | JSON 직렬화/역직렬화 |
| **Commons IO** | 2.16.1 | 파일 I/O 유틸리티 |

---

## 🏗 시스템 아키텍처

### 전체 시스템 구조

<!-- 여기에 시스템 아키텍처 다이어그램 추가 -->

### 도메인 모듈 구조

```
src/main/java/com/fairing/fairplay/
├── core/                        # 핵심 인프라 모듈
│   ├── config/                 # 설정 클래스
│   │   ├── SecurityConfig      # Spring Security 설정
│   │   ├── RedisConfig         # Redis 캐시 설정
│   │   ├── AwsConfig           # AWS S3 설정
│   │   ├── WebSocketConfig     # WebSocket 설정
│   │   └── QueryDslConfig      # QueryDSL 설정
│   ├── security/               # 보안 관련
│   │   ├── JwtTokenProvider    # JWT 토큰 생성/검증
│   │   ├── JwtAuthFilter       # JWT 인증 필터
│   │   ├── CustomUserDetails   # 사용자 상세 정보
│   │   └── FunctionAuth        # 커스텀 권한 어노테이션
│   ├── service/                # 핵심 서비스
│   │   ├── AuthService         # 인증 서비스
│   │   ├── EmailService        # 이메일 발송
│   │   └── FileUploadService   # 파일 업로드
│   └── exception/              # 전역 예외 처리
│       ├── GlobalExceptionHandler
│       └── Custom Exceptions
│
├── user/                        # 사용자 관리 도메인
│   ├── entity/
│   │   ├── Users               # 사용자 엔티티
│   │   ├── UserRoleCode        # 사용자 역할
│   │   ├── EventAdmin          # 행사 관리자
│   │   └── BoothAdmin          # 부스 관리자
│   ├── repository/
│   │   ├── UserRepository
│   │   └── UserRoleRepository
│   ├── service/
│   │   ├── UserService
│   │   └── RoleManagementService
│   └── controller/
│       └── UserController
│
├── event/                       # 행사 관리 도메인
├── reservation/                 # 예약 관리 도메인
├── qr/                          # QR 시스템 도메인
├── chat/                        # 채팅 시스템 도메인
├── ai/                          # AI 챗봇 도메인
├── payment/                     # 결제 시스템 도메인
├── statistics/                  # 통계 및 분석 도메인
├── admin/                       # 관리자 기능 도메인
├── booth/                       # 부스 관리 도메인
├── businesscard/                # 디지털 명함 도메인
├── review/                      # 리뷰 시스템 도메인
├── wishlist/                    # 위시리스트 도메인
├── notification/                # 알림 시스템 도메인
└── common/                      # 공통 유틸리티
```

---

## 🚀 설치 및 실행

### 요구사항

- **Java**: 21 이상 (JDK 21 권장)
- **Gradle**: 8.x 이상
- **MySQL**: 8.0 이상
- **Redis**: 6.x 이상

### 1. 저장소 클론

```bash
git clone https://github.com/rktclgh/FairPlay_BE.git
cd FairPlay_BE
```

### 2. 데이터베이스 설정

**MySQL 데이터베이스 생성**

```sql
CREATE DATABASE fairplay
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE USER 'fairplay_user'@'localhost'
IDENTIFIED BY 'your_password';

GRANT ALL PRIVILEGES ON fairplay.*
TO 'fairplay_user'@'localhost';

FLUSH PRIVILEGES;
```

**Redis 실행**

```bash
# Docker로 실행 (권장)
docker run -d \
  -p 6379:6379 \
  --name redis \
  redis:6-alpine

# 또는 로컬 설치 후
redis-server
```

### 3. 환경 변수 설정

프로젝트 루트에 `.env` 파일 생성:

```bash
# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/fairplay?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=fairplay_user
DB_PASSWORD=your_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-minimum-64-characters-long-for-hs512-algorithm
HASHIDS_SALT=your-hashids-salt-for-id-encoding

# Email Configuration (Gmail SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-gmail-app-password

# AWS S3 Configuration
AWS_S3_BUCKET_NAME=fair-play-bucket
AWS_ACCESS_KEY=your-aws-access-key
AWS_SECRET_KEY=your-aws-secret-key
AWS_REGION=ap-northeast-2
AWS_CLOUDFRONT_DOMAIN=https://your-cloudfront-domain.cloudfront.net

# Kakao OAuth
KAKAO_CLIENT_ID=your-kakao-client-id
KAKAO_REDIRECT_URI=http://localhost:5173/auth/kakao/callback

# Payment (Iamport)
IAMPORT_API_KEY=your-iamport-api-key
IAMPORT_SECRET_KEY=your-iamport-secret-key

# Google Gemini AI
GEMINI_API_KEY=your-gemini-api-key

# QR Ticket Configuration
QR_TICKET_REACT_URL=http://localhost:5173/qr-ticket/participant?token=
```

### 4. 빌드 및 실행

**환경 변수 로드 후 실행**

```bash
# .env 파일의 환경 변수를 export
export $(cat .env | xargs)

# Gradle로 빌드
./gradlew clean build

# 애플리케이션 실행
./gradlew bootRun
```

**또는 JAR 파일 직접 실행**

```bash
java -jar build/libs/fairplay-0.0.1-SNAPSHOT.jar
```

### 5. API 접속 확인

```bash
# 헬스 체크
curl http://localhost:8080/actuator/health

# 응답 예시
{
  "status": "UP"
}
```

**Swagger UI 접속**

```
http://localhost:8080/swagger-ui.html
```

### 6. 개발 모드 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

---

## 📚 API 문서

### Swagger UI

애플리케이션 실행 후 다음 URL에서 전체 API 문서 확인:

```
http://localhost:8080/swagger-ui.html
```

### 주요 API 엔드포인트

#### 인증 (Authentication)

```http
POST   /api/auth/login               # 이메일 로그인
POST   /api/auth/register            # 회원가입
POST   /api/auth/refresh-token       # JWT 토큰 갱신
GET    /api/auth/email-verify        # 이메일 인증
POST   /api/auth/kakao/login         # 카카오 소셜 로그인
POST   /api/auth/logout              # 로그아웃
```

#### 행사 (Events)

```http
GET    /api/events                   # 행사 목록 조회
POST   /api/events                   # 행사 생성 (EVENT_ADMIN)
GET    /api/events/{eventId}         # 행사 상세 조회
PUT    /api/events/{eventId}         # 행사 수정 (EVENT_ADMIN)
DELETE /api/events/{eventId}         # 행사 삭제 (EVENT_ADMIN)
GET    /api/events/popular           # 인기 행사 조회
GET    /api/events/category/{cat}    # 카테고리별 행사
```

#### 예약 (Reservations)

```http
POST   /api/reservations             # 예약 생성
GET    /api/reservations/{userId}    # 사용자 예약 목록
PUT    /api/reservations/{id}        # 예약 수정
DELETE /api/reservations/{id}        # 예약 취소
GET    /api/reservations/{id}/status # 예약 상태 조회
```

#### QR 티켓 (QR Tickets)

```http
GET    /api/qr/tickets/{ticketId}    # QR 티켓 조회
POST   /api/qr/check                 # QR 코드 검증
POST   /api/qr/check-in              # 입장 처리
POST   /api/qr/check-out             # 퇴장 처리
POST   /api/qr/reissue               # 티켓 재발급
GET    /api/qr/check-logs/{eventId}  # 입퇴장 로그 조회
```

#### 채팅 (Chat)

```http
WS     /ws/chat                      # WebSocket 연결
POST   /api/chat/rooms               # 채팅방 생성
GET    /api/chat/rooms/{roomId}/messages  # 채팅 내역
GET    /api/chat/rooms               # 채팅방 목록
DELETE /api/chat/rooms/{roomId}      # 채팅방 삭제
```

#### AI 챗봇 (AI Chat)

```http
POST   /api/ai/chat                  # AI 대화
POST   /api/admin/rag/ingest         # 문서 임베딩 (ADMIN)
GET    /api/admin/rag/documents      # 문서 목록 (ADMIN)
DELETE /api/admin/rag/documents/{id} # 문서 삭제 (ADMIN)
```

#### 관리자 (Admin)

```http
GET    /api/admin/dashboard          # 관리자 대시보드
GET    /api/admin/users              # 사용자 관리
POST   /api/admin/grant-event-admin/{userId}   # 행사 관리자 권한 부여
DELETE /api/admin/revoke-event-admin/{userId}  # 행사 관리자 권한 박탈
GET    /api/admin/statistics/*       # 통계 조회
```

### 인증 방법

모든 보호된 엔드포인트는 JWT 토큰이 필요합니다:

```http
Authorization: Bearer {your_jwt_token}
```

**예시 요청**

```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzAwMDAwMDAwLCJleHAiOjE3MDAwMDE4MDB9...." \
     http://localhost:8080/api/events
```

---

## 🗄️ 데이터베이스 구조

### ERD (Entity Relationship Diagram)

<!-- 여기에 ERD 다이어그램 이미지 추가 -->

### 주요 테이블

#### users (사용자)

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    profile_image_url VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',

    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### user_role_code (역할)

```sql
CREATE TABLE user_role_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    granted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_role (user_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### event (행사)

```sql
CREATE TABLE event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(50),
    location VARCHAR(255),
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    max_attendees INT,
    current_attendees INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_start_date (start_date),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 🐳 배포

### Docker 빌드

```bash
# JAR 빌드
./gradlew clean build

# Docker 이미지 빌드
docker build -t fairplay-backend:latest .

# Docker 컨테이너 실행
docker run -d \
  -p 8080:8080 \
  --env-file .env \
  --name fairplay-backend \
  fairplay-backend:latest
```

### GitHub Actions CI/CD

프론트엔드 푸시 시 자동으로 백엔드 배포가 트리거됩니다.

**배포 플로우**:
1. 프론트엔드 main 브랜치 푸시
2. 백엔드에 `repository_dispatch` 이벤트 전송
3. 백엔드에서 프론트엔드 소스 가져오기
4. 통합 빌드 및 Docker 이미지 생성
5. DockerHub에 푸시
6. EC2에서 자동 배포

---



## 👥 팀원

- **Backend**: 송치호, 김희연, 이예은, 강병찬, 안성현, 윤선현, 황인선
- **Frontend**: 송치호, 문정환
- **Design**: 문정환

---

## 📞 문의

- **이메일**: songchih@icloud,com
- **GitHub**: [Issues](https://github.com/rktclgh/FairPlay_BE/issues)

---

**Made with ❤️ by FairPlay Team**
