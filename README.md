# 박람회 예약/관리 플랫폼 (Fairplay)

> Spring Boot 기반의 멀티테넌트 박람회/행사 예약 관리 백엔드 프로젝트
> ⚡️ .env 파일을 통한 환경변수 분리, 인텔리제이(IDEA) 개발환경 기준

---

## 🗂️ 프로젝트 디렉토리 구조
```
fairplay/
├── .env                  # (필수) 환경변수 파일 - 루트에 위치, git에 올리지 말 것
├── .gitignore
├── README.md
├── build.gradle
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/fairing/fairplay/
│   │   │        ├── admin/          # 관리자(관리자 기능/권한)
│   │   │        ├── common/         # 공통 유틸리티/상수/예외처리 등
│   │   │        ├── core/           # 핵심 도메인/시스템 공통코어(멀티테넌시 등)
│   │   │        ├── event/          # 박람회/행사 도메인(생성, 조회, 관리)
│   │   │        ├── payment/        # 결제, 포인트 관리 등 결제 시스템
│   │   │        ├── reservation/    # 예약(신청/취소/조회/승인 등)
│   │   │        └── user/           # 회원/계정(가입, 인증, 마이페이지 등)
│   │   └── resources/
│   │       └── application.yml      # 환경설정(변수는 .env로 치환)
│   └── test/
└── ...

```

## ⚙️ 환경설정 및 실행 방법

### 1. `.env` 파일 생성 (루트 폴더에 위치, 예시)

DB\_URL=jdbc\:mysql://localhost:3306/fairplay?useSSL=false\&serverTimezone=Asia/Seoul\&characterEncoding=UTF-8
DB\_USERNAME=root
DB\_PASSWORD=password

# 추가 환경변수 작성

> ⚠️ `.env` 파일은 반드시 프로젝트 루트(최상단)에 위치해야 합니다.
> ⚠️ `.env` 파일은 절대 git에 올리지 말고, 팀원 개별 배포하세요.

---

### 2. 인텔리제이에서 실행 방법

1. `.env` 파일을 프로젝트 루트에 추가
2. `build.gradle` 동기화 (필요시)
3. 실행 전 .env 환경변수 등록:

    * 터미널에서
      export \$(cat .env | xargs)
      ./gradlew bootRun
    * 또는
      인텔리제이 Run/Debug 설정 > 환경변수 → `.env` 파일의 내용 복사해서 붙여넣기
4. 정상적으로 `application.yml` 내 `${DB_URL}` 등이 치환되어 실행됨

---

### 3. 주요 작업/커밋 규칙

* 환경별 민감정보는 `.env`로만 관리, git에는 올리지 않습니다.
* 코드/설정 변경 시 `application.yml`과 `.env`(예시) 동기화 권장
* 신규 라이브러리 추가 시 반드시 `build.gradle`에 명시
* 실행 전 항상 `.env` 파일이 최신/필수 값 포함하는지 확인
* 로컬 개발은 인텔리제이 기준, 기타 IDE 지원 X

---

### 4. `.gitignore` 및 보안 지침

* `.env`, `.idea/`, `/build/`, `.DS_Store` 등은 git에 커밋하지 않습니다.
* 이미 커밋된 민감 파일은 git history에서 완전 삭제(팀장/관리자와 협의)
* 팀원에게 .env 파일 직접 전달

---

### 5. 주요 디렉토리/파일 설명

| 경로                                    | 설명              |
| ------------------------------------- | --------------- |
| `/src/main/java/...`                  | Java 소스 코드      |
| `/src/main/resources/application.yml` | 스프링 환경설정        |
| `/build.gradle`                       | Gradle 빌드 설정    |
| `/.env`                               | 환경별 민감정보 (DB 등) |
| `/.gitignore`                         | git 제외 파일 설정    |
| `/logs/`, `/build/`                   | 빌드 및 로그 산출물     |

---

## 🛠️ 개발/운영 참고사항

* 로컬 DB는 MySQL 8.x 이상 권장, 포트/DB명 `.env`로 설정
* 자바 버전은 21로 고정(Java 17 미만 지원 X)
* 개발 환경은 인텔리제이 최신판 기준(다른 IDE 문제 발생 시 지원 불가)
* 서버/운영 배포 시 `.env` 환경변수 export 후 실행

---

## 💡 FAQ & 문제해결

* 실행 시 DB 접속 오류

    * `.env` 파일 경로/내용 확인
    * DB 서버 정상 실행 여부 체크
* 환경변수 치환 안됨

    * `.env` export 여부 및 application.yml `${...}` 문법 오타 확인
    * 인텔리제이 실행 환경변수 세팅 재확인

---

---

> ⚡️ 최신 세팅법이나 추가 작업지시/개발규칙은 이 README에 계속 추가합니다.!
