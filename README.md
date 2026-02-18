# MobileFactory Lotto Assignment

로또 이벤트 시스템 과제용 프로젝트입니다.  
Vue 3 프론트엔드와 Spring Boot(MyBatis) 백엔드, MariaDB를 사용합니다.

- Backend: Spring Boot 3.5.10 / Java 17 / MyBatis
- Frontend: Vue 3 + Vite / TypeScript / Vue Router
- DB: MariaDB (InnoDB)
- DB 접속 정보는 환경변수(DB_URL, DB_USERNAME, DB_PASSWORD)로 주입합니다.

---

## Repository Structure

- backend/ : Spring Boot API 서버
- frontend/ : Vue 웹 클라이언트
- db/schema.sql : DB 스키마 생성 스크립트(DROP/CREATE 포함)
- docs/ : 설계 문서 및 ERD(선택)

---

## Prerequisites

- Java 17
- MariaDB
- Node.js (권장: LTS)
- (선택) DBeaver

---

## Database Setup

1. MariaDB에서 데이터베이스/계정을 준비합니다.

예시(로컬 기준):

- DB: lotto_event
- User: lotto_app

2. 스키마 적용

DBeaver에서 `db/schema.sql` 내용을 실행하거나, CLI로 실행합니다.

CLI 예시:

```bash
mysql -u lotto_app -p lotto_event < db/schema.sql
```

주의

- db/schema.sql에는 DROP TABLE IF EXISTS가 포함되어 있어 기존 테이블/데이터가 삭제될 수 있습니다.

## Backend Setup (Spring Boot)

1. 환경변수 설정

backend/src/main/resources/application.yml은 아래 환경변수를 참조합니다.

- DB_URL
- DB_USERNAME
- DB_PASSWORD

Windows PowerShell 예시
$env:DB_URL="jdbc:mariadb://localhost:3306/lotto_event?serverTimezone=Asia/Seoul&characterEncoding=utf8"
$env:DB_USERNAME="lotto_app"
$env:DB_PASSWORD="your_password"

2. 실행

방법 A) 터미널에서 실행
cd backend
./gradlew bootRun

방법 B) VSCode 실행

- Spring Boot Dashboard에서 LottoApplication 실행

참고

- 환경변수 값은 커밋하지 않습니다.
- application.yml에는 ${DB_URL}, ${DB_USERNAME}, ${DB_PASSWORD} 형태로만 두는 방식을 사용합니다.

## Frontend Setup (Vue)

1. 의존성 설치
   cd frontend
   npm install

2. 개발 서버 실행
   npm run dev

참고

- 프론트는 Vue 3 + Vite + TypeScript + Vue Router 기반입니다.
- API 호출은 백엔드 실행 후 확인합니다.

## Current Progress

### ✅ 완료된 기능

#### 백엔드 구현
- **DB 스키마 작성 및 적용 완료** (event/participant/ticket/prize/sms_log)
- **공통 인프라**:
  - ApiResponse 래퍼 (성공/실패 통합 응답)
  - GlobalExceptionHandler (전역 예외 처리)
  - ErrorCode 열거형 (에러 코드 관리)
- **참여 API** (`POST /api/participations`):
  - 이벤트 기간 검증
  - 참여 인원 제한 (10,000명)
  - 중복 참여 방지
  - 6자리 랜덤 로또 번호 발급
  - SMS 로그 기록
- **결과 조회 API** (`POST /api/results/check`):
  - 발표 기간 검증
  - 첫 조회: 당첨 등수 공개
  - 두 번째 이후 조회: 당첨/미당첨 여부만 표시
  - 조회 횟수 및 타임스탬프 관리
- **관리자 API - 당첨 산정** (`POST /api/admin/events/{eventId}/draw`):
  - **하이브리드 방식**: 로또 번호 연속 자리수 일치 + Fallback
  - 1등: fixedFirstPhoneHash 지정 또는 2000~7000번 중 선택
  - 2등 5명: 2000~7000번 중 5자리 일치 우선
  - 3등 44명: 1000~8000번 중 4자리 일치 우선
  - 4등 950명: 전체 중 3자리 일치 우선
  - 멱등성 보장 (재실행 시 기존 결과 반환)
- **관리자 API - 미확인 당첨자 안내** (`POST /api/admin/events/{eventId}/remind-unconfirmed`):
  - 발표일 +10일 경과 확인
  - check_count = 0인 당첨자 대상
  - 중복 발송 방지 (날짜별)
  - SMS 로그 기록

#### 프론트엔드
- Vue 3 + Vite + TypeScript 프로젝트 스캐폴드 생성
- Vue Router 설정 완료

#### 문서
- **API 명세서** (`doc/API.md`):
  - 참여/결과 조회/관리자 API 스펙 정의
  - 당첨 로직 상세 설명 (하이브리드 방식)
  - 요청/응답 예시 및 에러 코드
- **ERD**: docs/erd.png (예정 또는 완료)

### 🔧 기술 스택 상세

**Backend**:
- Spring Boot 3.5.10, Java 17
- MyBatis 3.0.5 (XML 매퍼)
- Lombok (생성자 주입)
- Jakarta Validation
- Transaction 관리 (@Transactional)

**Frontend**:
- Vue 3 Composition API
- TypeScript
- Vite
- Vue Router

**Database**:
- MariaDB (InnoDB)
- SHA-256 해시 기반 전화번호 보안
- 인덱스: phone_hash, event_id, participant_id

### 📋 API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/participations` | 로또 이벤트 참여 |
| POST | `/api/results/check` | 당첨 결과 조회 |
| POST | `/api/admin/events/{eventId}/draw` | 당첨 산정 실행 |
| POST | `/api/admin/events/{eventId}/remind-unconfirmed` | 미확인 당첨자 안내 |

상세 API 스펙은 `doc/API.md` 참조.

### 🎯 핵심 구현 포인트

1. **당첨 로직 (하이브리드 방식)**:
   - 1등 당첨자의 번호를 당첨 번호로 설정
   - 연속 자리수 일치 우선 선택 (6자리→5자리→4자리→3자리)
   - 일치자 부족 시 후보군에서 랜덤 선택으로 정확한 당첨자 수 보장

2. **멱등성 보장**:
   - 당첨 산정: 이미 1,000개 Prize 존재 시 재산정 없이 결과 반환
   - 안내 발송: 날짜별 중복 발송 방지

3. **보안**:
   - 전화번호는 SHA-256 해시로 저장 (평문 미저장)
   - phone_hash로 중복 참여 및 결과 조회 처리

4. **비즈니스 로직**:
   - 이벤트 기간 검증 (참여: eventStart~eventEnd / 발표: announceStart~announceEnd)
   - 참여 인원 제한 (최대 10,000명)
   - 결과 조회 정책 (1회: 등수 공개 / 2회 이상: 당첨 여부만)

### 📝 간소화 사항 (주니어 과제용)

- SMS 발송 실제 연동 없음 (로그만 기록)
- 프론트엔드는 스캐폴드만 생성 (UI 미구현)
- 인증/인가 없음 (관리자 API 보안 없음)
- 단일 이벤트 (eventId=1) 가정
- 연속 자리수 일치 방식 (순서 무관 매칭 대신 구현 단순화)

## Notes

- 문자 발송(SMS/카카오)은 외부 연동 없이 Mock 처리합니다.
  - 번호는 API 응답/화면에 표시
  - 발송 이력은 sms_log 테이블에 기록하는 방식으로 설계합니다.
- 휴대폰 번호 원문은 저장하지 않고 해시(phone_hash)로 저장하는 방향을 사용합니다.
- 과제 제출을 위해 README에 로컬 실행 재현 절차를 우선 정리합니다.
