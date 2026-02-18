# Lotto Event API Specification

본 문서는 로또 이벤트 시스템의 API 스펙을 정의합니다.

---

## 1. 참여 API

### 1.1. 로또 이벤트 참여

**Endpoint**: `POST /api/participations`

**설명**: 사용자가 휴대폰 번호로 로또 이벤트에 참여합니다.

**Request Body**:

```json
{
  "phone": "010-1234-5678"
}
```

**Validation**:

- `phone`: 필수, 10~13자리 숫자 및 하이픈 허용

**Response (200 OK)**:

```json
{
  "success": true,
  "data": {
    "participantId": 1234,
    "lottoNumber": "123456"
  },
  "error": null
}
```

**Error Responses**:

- `400 INVALID_REQUEST`: 휴대폰 번호 형식이 올바르지 않음
- `400 EVENT_NOT_ACTIVE`: 이벤트 기간이 아님 (eventStart ~ eventEnd 범위 외)
- `400 CAPACITY_FULL`: 참여 인원 초과 (기본 10,000명)
- `400 DUPLICATE_PARTICIPATION`: 이미 참여한 번호
- `500 INTERNAL_ERROR`: 서버 내부 오류

---

## 2. 결과 조회 API

### 2.1. 당첨 결과 조회

**Endpoint**: `POST /api/results/check`

**설명**: 참여자가 휴대폰 번호로 당첨 여부를 확인합니다.

**Request Body**:

```json
{
  "phone": "010-1234-5678"
}
```

**Validation**:

- `phone`: 필수, 10~13자리 숫자 및 하이픈 허용

**Response (200 OK - 첫 조회)**:

```json
{
  "success": true,
  "data": {
    "rank": 1,
    "isWinner": null,
    "lottoNumber": "123456",
    "amount": 5000000
  },
  "error": null
}
```

**Response (200 OK - 두 번째 이후 조회)**:

```json
{
  "success": true,
  "data": {
    "rank": null,
    "isWinner": true,
    "lottoNumber": "123456",
    "amount": 5000000
  },
  "error": null
}
```

**Response (200 OK - 낙첨자)**:

```json
{
  "success": true,
  "data": {
    "rank": null,
    "isWinner": false,
    "lottoNumber": "123456",
    "amount": null
  },
  "error": null
}
```

**Error Responses**:

- `400 INVALID_REQUEST`: 휴대폰 번호 형식이 올바르지 않음
- `400 ANNOUNCE_NOT_ACTIVE`: 발표 기간이 아님 (announceStart ~ announceEnd 범위 외)
- `404 NOT_FOUND`: 해당 번호로 참여 내역이 없음
- `500 INTERNAL_ERROR`: 서버 내부 오류

**비즈니스 로직**:

- 첫 조회 시: `rank` 필드에 등수 또는 null (낙첨) 반환
- 두 번째 이후 조회: `isWinner` 필드에 true/false 반환
- `check_count` 자동 증가 및 `first_checked_at`, `last_checked_at` 타임스탬프 업데이트

---

## 3. 관리자 API (Admin)

### 3.1. 당첨 산정 (Draw)

**Endpoint**: `POST /api/admin/events/{eventId}/draw`

**설명**: 이벤트 종료 후 당첨자를 산정하고 `prize` 테이블에 저장합니다.

**Path Parameters**:

- `eventId`: 이벤트 ID (예: 1)

**Request Body**: 없음

**Response (200 OK)**:

```json
{
  "success": true,
  "data": {
    "eventId": 1,
    "totalWinners": 1000,
    "firstPrizeCount": 1,
    "secondPrizeCount": 5,
    "thirdPrizeCount": 44,
    "fourthPrizeCount": 950
  },
  "error": null
}
```

**Error Responses**:

- `400 INVALID_REQUEST`: 이벤트가 존재하지 않거나, 이미 산정이 완료된 경우
- `500 INTERNAL_ERROR`: 서버 내부 오류

**비즈니스 로직**:

1. **멱등성 보장**: 이미 1,000개의 `prize` 레코드가 존재하는 경우, 재산정 없이 현재 결과를 반환

2. **당첨 번호 결정**:
   - `event.fixed_first_phone_hash`와 일치하는 참여자의 로또 번호를 **당첨 번호**로 설정
   - 해당 참여자가 없는 경우: 참여 번호 2000~7000번 중 랜덤 1명 선택하여 그의 번호를 당첨 번호로 지정

3. **당첨자 산정 규칙** (연속 자리수 일치):

   **자리수 일치 정의**: 당첨 번호와 참가자의 로또 번호를 앞자리부터 비교하여 연속 일치 자릿수 확인
   - 예: 당첨 번호 `"123456"`
     - 6자리 일치: `"123456"` (1등)
     - 5자리 일치: `"12345X"` (X는 6 제외) (2등 후보)
     - 4자리 일치: `"1234XX"` (3등 후보)
     - 3자리 일치: `"123XXX"` (4등 후보)

   **등수별 선정 로직**:
   - **1등 (1명)**: 당첨 번호의 소유자 (6자리 일치)
   - **2등 (5명)**:
     - 후보군: 참여 번호 2000~7000번 (1등 제외)
     - 우선순위 1: 후보군 중 5자리 일치자 검색
     - 우선순위 2: 5자리 일치자가 5명보다 많으면 랜덤 5명 선택
     - 우선순위 3: 5자리 일치자가 5명보다 적으면, 부족 인원은 후보군에서 랜덤 선택 (번호 무관)
   - **3등 (44명)**:
     - 후보군: 참여 번호 1000~8000번 (1등, 2등 제외)
     - 우선순위 1: 후보군 중 4자리 일치자 검색
     - 우선순위 2: 4자리 일치자가 44명보다 많으면 랜덤 44명 선택
     - 우선순위 3: 4자리 일치자가 44명보다 적으면, 부족 인원은 후보군에서 랜덤 선택 (번호 무관)
   - **4등 (950명)**:
     - 후보군: 전체 참여자 (1~3등 제외)
     - 우선순위 1: 후보군 중 3자리 일치자 검색
     - 우선순위 2: 3자리 일치자가 950명보다 많으면 랜덤 950명 선택
     - 우선순위 3: 3자리 일치자가 950명보다 적으면, 부족 인원은 후보군에서 랜덤 선택 (번호 무관)

4. **트랜잭션**: 모든 당첨 정보는 단일 트랜잭션으로 삽입 (실패 시 전체 롤백)

**설계 의도** (하이브리드 방식):

- **로또 번호 매칭 로직 유지**: 실제 로또처럼 번호 일치 기반 당첨 개념을 구현
- **정확한 당첨자 수 보장**: 일치자 부족 시 Fallback 로직으로 정확히 1,000명 선정
- **모든 제약 조건 충족**: 1등 사전 지정, 2등/3등 참여 번호 범위 조건 모두 만족
- **주니어 과제 적합성**: 복잡한 순열/조합 대신 연속 자리수 비교로 구현 간소화

---

### 3.2. 미확인 당첨자 안내 (Unconfirmed Winner Reminder)

**Endpoint**: `POST /api/admin/events/{eventId}/remind-unconfirmed`

**설명**: 발표일로부터 10일이 경과한 시점에 `check_count = 0`인 당첨자에게 SMS 안내를 발송합니다.

**Path Parameters**:

- `eventId`: 이벤트 ID (예: 1)

**Request Body**: 없음

**Response (200 OK)**:

```json
{
  "success": true,
  "data": {
    "eventId": 1,
    "targetDate": "2025-02-05",
    "totalUnconfirmedWinners": 123,
    "remindersSent": 123,
    "remindersFailed": 0
  },
  "error": null
}
```

**Error Responses**:

- `400 INVALID_REQUEST`: 이벤트가 존재하지 않거나, 발표일로부터 10일 미경과
- `500 INTERNAL_ERROR`: 서버 내부 오류

**비즈니스 로직**:

1. **실행 조건**: `event.announce_start + 10일 <= 현재 날짜`
2. **대상 선정**:
   - `prize` 테이블에 존재하는 당첨자 중
   - `participant.check_count = 0` (한 번도 조회하지 않은 사람)
3. **SMS 발송**:
   - 대상자의 `phone_hash`로 SMS 발송 (실제로는 모킹)
   - `sms_log` 테이블에 `type = 'UNCONFIRMED_WINNER_REMINDER'`, `status = 'SENT'` 기록
4. **멱등성 부분 보장**:
   - 동일 날짜에 중복 발송 방지: `sms_log`에서 `sent_date = 현재 날짜`, `type = 'UNCONFIRMED_WINNER_REMINDER'` 인 레코드가 있는 경우 스킵
   - 이미 발송된 대상자는 제외

**간소화 사항** (주니어 개발자 과제용):

- 실제 SMS 발송 없음 (로그만 기록)
- 발송 실패 케이스는 별도 처리하지 않음 (모두 SENT로 기록)

---

## 4. 공통 Error Response 형식

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error message description"
  }
}
```

---

## 5. 데이터 타입 및 제약 사항

### 5.1. 상수 정의

- **최대 참여 인원**: `event.max_participants` (기본값 10,000)
- **로또 번호 형식**: 6자리 숫자 문자열 (`000000` ~ `999999`)
- **당첨자 수**: 총 1,000명 (1등 1명, 2등 5명, 3등 44명, 4등 950명)

### 5.2. 보안

- 휴대폰 번호는 SHA-256 해시로 저장 (`phone_hash` 필드)
- 평문 전화번호는 DB에 저장하지 않음

### 5.3. 날짜 타입

- `event_start`, `event_end`, `announce_start`, `announce_end`: `DATE` (LocalDate)
- `created_at`, `updated_at`, `first_checked_at`, `last_checked_at`: `DATETIME` (LocalDateTime)
- `sent_date`: `DATE` (LocalDate)
