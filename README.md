# MobileFactory Lotto Assignment - 로또 이벤트 시스템

자사 홈페이지 접속 고객을 대상으로 로또 번호를 무료 발번하고, 당첨일에 번호 확인 및 경품 지급이 가능한 이벤트 시스템입니다.

## 개요

- 이벤트 기간: 2025/02/01 ~ 2025/03/31
- 발표 기간: 2025/04/01 ~ 2025/04/15
- 최대 참가자: 10,000명
- 총 당첨자: 1,000명 (1등 1명, 2등 5명, 3등 44명, 4등 950명)

## 구현 범위

본 과제에서는 전체 요구사항 중 다음 영역을 중심으로 구현했습니다.

- 이벤트 참여 및 중복 참여 방지
- 로또 번호 발번 및 사전 풀 관리
- 당첨자 분배 로직 및 조건 제약 처리
- 결과 확인 정책 (최초 조회 / 재조회 분리)
- 미확인 당첨자 알림 스케줄링
- 테스트를 위한 날짜 제어 시스템

문자 발송 및 휴대폰 인증은 실제 외부 연동 대신,
발송 이력을 DB에 기록하는 방식으로 시스템 구조와 처리 흐름 중심으로 구현했습니다.

## 기술 스택

- Backend: Spring Boot 3.5.10 / Java 17 / MyBatis
- Frontend: Vue 3 + Vite / TypeScript / Vue Router
- Database: MariaDB (InnoDB)

## 빠른 시작 (로컬 개발)

### 1) 사전 준비

- Java 17 이상
- Node.js (LTS)
- MariaDB 10.6+

### 2) 데이터베이스 설정

```sql
-- MariaDB에 로또 DB 생성
CREATE DATABASE lotto_event CHARACTER SET utf8mb4;
CREATE USER 'lotto_app'@'localhost' IDENTIFIED BY 'passw0rd';
GRANT ALL PRIVILEGES ON lotto_event.* TO 'lotto_app'@'localhost';
```

```bash
# 스키마 적용
mysql -u lotto_app -p lotto_event < db/schema.sql
```

### 3) 환경변수 설정 (Windows PowerShell)

```powershell
$env:DB_URL="jdbc:mariadb://localhost:3306/lotto_event?serverTimezone=Asia/Seoul&characterEncoding=utf8"
$env:DB_USERNAME="lotto_app"
$env:DB_PASSWORD="passw0rd"

# 테스트 날짜 설정 (선택 - 이벤트 기간 내)
$env:TEST_CURRENT_DATE="2025-02-15"
```

### 4) 백엔드 실행

```bash
cd backend
.\gradlew bootRun
# 성공 시: http://localhost:8080
```

### 5) 프론트엔드 실행 (새 터미널)

```bash
cd frontend
npm install
npm run dev
# 성공 시: http://localhost:5173
```

### 6) 웹 테스트

1. http://localhost:5173 이벤트 페이지 접속
2. 휴대폰 번호 입력 (예: 010-1234-5678)
3. 로또 번호 수령
4. 결과 조회 페이지 이동
5. 당첨 여부 확인

## 주요 API

### 참여 API

```http
POST /api/participations
{
  "phone": "010-1234-5678"
}

응답:
{
  "success": true,
  "data": {
    "participantId": 1,
    "lottoNumber": "3,11,22,33,41,45"
  }
}
```

### 결과 조회 API

```http
POST /api/results/check
{
  "phone": "010-1234-5678"
}

첫 조회: { "rank": 2, "checkCount": 1 }
두 번째 이후: { "isWinner": true, "checkCount": 2 }
```

### 관리자 API

※ 관리자 API는 이벤트 운영 및 테스트를 위한 내부용 API입니다.

eventId는 DB의 event 테이블 또는 초기 데이터 기준 값을 사용합니다.

```http
POST /api/admin/events/{eventId}/prepare-tickets  # 번호 생성
POST /api/admin/events/{eventId}/draw             # 당첨 산정
POST /api/admin/events/{eventId}/remind-unconfirmed  # 알림 발송
```

## 핵심 기능

본 시스템은 최대 10,000명 규모의 이벤트를 고려하여 번호 사전 생성 + 풀 기반 배정 방식으로 설계되었습니다.

### 1) 로또 번호 생성

- 사전 생성: 애플리케이션 시작 시 10,000개 번호 미리 생성
- Rank 배분: 1등(1) / 2등(5) / 3등(44) / 4등(950) / 비당첨(9,000)
- Seq 제약: 2등(2000~7000) / 3등(1000~8000) / 1,4등(1~10000)

### 2) 특정 휴대폰 1등 보장

- fixed_first_phone_hash로 지정된 휴대폰이 참여하면 1등 번호 배정
- 현재 설정: SHA2('01012345678', 256)

### 3) 중복 참여 방지

- 휴대폰 해시(phone_hash) 기반 UNIQUE 제약
- 휴대폰 형식 정규화 (010-1234-5678 = 01012345678)

### 4) 결과 조회 정책

- 첫 조회: 당첨 등수 공개 (rank: 1~4 또는 null)
- 재조회: 당첨/미당첨만 표시 (isWinner: true/false)

### 5) 미확인 당첨자 알림

- 스케줄러: 매일 자정 자동 실행
- 조건: 발표 시작일 + 10일 경과 + check_count=0
- 중복 방지: 날짜별 SMS 로그 확인

## 단위 테스트

```bash
cd backend
.\gradlew test

# 특정 테스트만 실행
.\gradlew test --tests TicketPoolServiceImplTest
.\gradlew test --tests ParticipationServiceImplTest
.\gradlew test --tests ResultCheckServiceImplTest
```

## 날짜 제어 시스템

테스트를 위한 날짜 제어 구현:

```java
// CurrentDateProvider.java
public LocalDate today() {
    String testDate = System.getenv("TEST_CURRENT_DATE");
    return testDate != null ? LocalDate.parse(testDate) : LocalDate.now();
}
```

사용 예:

```powershell
# 이벤트 기간
$env:TEST_CURRENT_DATE="2025-02-15"

# 발표 기간
$env:TEST_CURRENT_DATE="2025-04-05"

# 발표 +10일 (알림 발송 테스트)
$env:TEST_CURRENT_DATE="2025-04-11"
```

## 최종 완료 현황

### 백엔드

- DB 스키마 완성
- 핵심 API 구현 (참여, 결과 조회, 관리자)
- 비즈니스 로직 전체
- 스케줄러 (미확인 당첨자 알림)
- 날짜 제어 시스템 (TEST_CURRENT_DATE)
- DummyController 제거
- 단위 테스트 3개
- 전역 예외 처리

### 프론트엔드

- Vue 3 프로젝트
- 라우터 설정
- API 호출 추상화
- 이벤트/결과 페이지

## 레포지토리 파일

- backend/: Spring Boot API 서버
- frontend/: Vue 3 클라이언트
- db/schema.sql: DB 스키마

## 트러블슈팅

Q: 이벤트 기간이 아닙니다 에러

```powershell
$env:TEST_CURRENT_DATE="2025-02-15"
```

Q: 포트 8080 사용 중

```bash
netstat -ano | find "8080"
taskkill /PID <PID> /F
```

Q: 환경변수 설정 안됨

```powershell
$env:DB_URL="jdbc:mariadb://localhost:3306/lotto_event?serverTimezone=Asia/Seoul&characterEncoding=utf8"
```

## 설계 및 구현 문서

본 README에서 요약한 내용의 상세 설계 및 구현 근거는 아래 문서에 정리되어 있습니다.

- 요구사항 해석 및 설계 방향
- DB 설계 및 ERD
- 당첨 로직 및 상태 관리 흐름
- 이벤트 전체 처리 시나리오

Notion 문서: https://clumsy-failing-2f1.notion.site/308b5958ea4b80398f7bddaeaf52a53f?source=copy_link
