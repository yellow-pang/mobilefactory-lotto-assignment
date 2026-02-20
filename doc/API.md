# Lotto Event API Specification

본 문서는 로또 이벤트 시스템의 API 스펙을 정의합니다.

---

## 1. 참여 API

### 1.1. 로또 이벤트 참여

**Endpoint**: `POST /api/participations`

**설명**: 사용자가 휴대폰 번호로 로또 이벤트에 참여합니다.

**휴대폰 인증 플로우** (프론트엔드):

1. 사용자가 휴대폰 번호 입력
2. "인증번호 전송" 버튼 클릭 → Mock 인증번호 생성 (6자리)
3. 인증번호 입력 및 확인
4. 인증 성공 후 참여 API 호출

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
    "lottoNumber": "3,11,22,33,41,45"
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

**휴대폰 인증 플로우** (프론트엔드):

1. 사용자가 휴대폰 번호 입력
2. "인증번호 전송" 버튼 클릭 → Mock 인증번호 생성 (6자리)
3. 인증번호 입력 및 확인
4. 인증 성공 후 결과 조회 API 호출

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
    "lottoNumber": "3,11,22,33,41,45",
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
    "lottoNumber": "3,11,22,33,41,45",
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
    "lottoNumber": "3,11,22,33,41,45",
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

2. **당첨 번호 생성 및 결정**:

- 1등 당첨 번호는 설정값(`lottery.first-prize-numbers`)으로 고정
- 1등 당첨 전화번호(`lottery.first-prize-phone`)가 참여하면 해당 번호를 발급
- 예: 설정값이 "3,11,22,33,41,45"이면 동일 번호를 받은 참가자가 1등

3. **당첨자 산정 규칙** (생성된 당첨 번호 기준):

   **1등 (1명)**: 당첨 번호를 정확히 일치하는 사람

- 설정된 당첨 번호 = "3,11,22,33,41,45"을 발급받은 참가자

**2등 (5명)**:

- 참여자 번호 2000~7000 범위 내에서만 선정
- 당첨 번호와 5개 일치자에서 정확히 5명 선정 (부족 시 에러)

**3등 (44명)**:

- 참여자 번호 1000~8000 범위 내에서만 선정
- 당첨 번호와 4개 일치자에서 정확히 44명 선정 (부족 시 에러)

**4등 (950명)**:

- 모든 참가자 중 3개 일치자 우선 선택
- 부족하면 2개, 1개... 순으로 보충하여 정확히 950명 선정

4. **확률적 균등 분배**:

- 참여 시점에 번호를 역설계하여 각 등수 인원을 충족
- 당첨 번호는 고정값이므로 자리수 일치 개수로 등수 산정
- 2등/3등 범위를 벗어난 당첨자는 허용하지 않음

5. **트랜잭션**: 모든 당첨 정보는 단일 트랜잭션으로 삽입 (실패 시 전체 롤백)

**설계 의도**:

- **실제 로또 방식 준수**: 당첨 번호가 draw 시 결정되고, 참가자들의 번호와 매칭
- **공정한 분배**: 모든 참가자가 동등한 확률로 번호 부여
- **논리적 정확성**: 번호 → 사람 → 등수 분배 순서로 진행
- **멱등성 보장**: 중복 draw 호출 시 첫 draw 결과 유지

  **일치 개수 정의**: 당첨 번호와 참가자 번호의 공통 숫자 개수
  - 예: 당첨 번호 "3,11,22,33,41,45"
    - "3,11,22,33,41,45": 6개 일치 (1등)
    - "3,11,22,33,41,7": 5개 일치 (2등 후보)
    - "3,11,22,33,9,7": 4개 일치 (3등 후보)
    - "3,11,22,8,9,7": 3개 일치 (4등 후보)
    - "1,2,4,5,6,7": 0개 일치 (낙첨)

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

---

## 6. 프론트엔드 기능

### 6.1. 매일 최초 접속 자동 오픈

**기능**: 사용자가 매일 최초로 서비스에 접속할 때 자동으로 이벤트 참여 페이지로 이동합니다.

**구현 방식**:

- localStorage를 사용하여 마지막 방문 날짜(`lotto_last_visit_date`) 저장
- 접속 시 저장된 날짜와 현재 날짜 비교
- 다른 날짜이면 최초 접속으로 간주

**동작 흐름**:

1. 사용자가 서비스 접속
2. App.vue에서 localStorage 확인
3. 최초 접속이면:
   - 마지막 방문 날짜 업데이트
   - 자동으로 `/event` 페이지로 라우팅
   - **"🎊 매일 최초 접속을 환영합니다!"** 녹색 메시지 표시 (EventView / ResultView)
4. 같은 날 재접속이면 normal flow 진행

**localStorage 데이터**:

```javascript
// 저장 형식
localStorage.setItem("lotto_last_visit_date", "2026-02-20"); // YYYY-MM-DD

// 조회 예시
const lastDate = localStorage.getItem("lotto_last_visit_date");
```

**데이터 구조**:

- Key: `lotto_last_visit_date`
- Value: `YYYY-MM-DD` 형식 문자열 (ISO date)
- 유효 기간: 매 자정(00:00)에 초기화

**UI 효과**:

- 환영 메시지는 slideDown 애니메이션으로 부드럽게 등장
- 참여/결과조회 페이지 상단에 위치
- ❌ 버튼으로 메시지 닫기 가능 (closable)

**주의사항**:

- 브라우저의 개인 정보 보호 모드에서는 localStorage가 제한될 수 있음
- 각 브라우저별로 별도의 localStorage 사용 (크로스 브라우저 추적 불가)
- 수동으로 브라우저 캐시 삭제 시 최초 접속으로 재인식

---

### 6.2. 이벤트 안내 모달

**기능**: 매일 최초 접속 시 이벤트 기간 및 발표 기간을 알려주는 **모달 팝업**이 자동으로 표시됩니다.

**구현 방식**:

- PrimeVue Dialog 컴포넌트 사용
- 최초 접속 시(`isFirstVisit.value === true`) 자동 오픈
- 사용자가 "확인" 버튼 클릭 시 모달 닫기

**모달 내용**:

```
┌─────────────────────────────────┐
│   🎯 로또 이벤트 안내            │
├─────────────────────────────────┤
│ 환영합니다.                     │
│ 전화번호를 입력하시면            │
│ 로또 번호를 전달 드립니다.      │
│                                 │
│ ┌───────────────────────────┐   │
│ │ 이벤트 기간                │   │
│ │ 2025/02/01 ~ 2025/03/31  │   │
│ │                           │   │
│ │ 발표 기간                  │   │
│ │ 2025/04/01 ~ 2025/04/15  │   │
│ └───────────────────────────┘   │
│                                 │
│ 기간안에 많은 참여 부탁드려요~! │
│                                 │
│           [✓ 확인]              │
└─────────────────────────────────┘
```

**표시되는 정보**:

- **설명 문구**: "환영합니다. 전화번호를 입력하시면 로또 번호를 전달 드립니다."
- **이벤트 기간**: `eventStart` ~ `eventEnd`
- **발표 기간**: `announceStart` ~ `announceEnd`
- **참여 안내 메시지**: "기간안에 많은 참여 부탁드려요~!"

**UI 특징**:

- **스타일**:
  - 모달 너비: 90vw (최대 500px)
  - 배경: 반투명 검은색 오버레이
  - 헤더 아이콘: 🎯
- **정보 섹션**:
  - 파란색 왼쪽 테두리 (4px, #3b82f6)
  - 라이트 블루 배경
  - 기간 정보 강조

- **메시지 섹션**:
  - 녹색 배경 (rgba(34, 197, 94, 0.05))
  - 녹색 텍스트 (#16a34a)
  - 중앙 정렬

**동작 흐름**:

1. 사용자가 매일 최초 접속
2. `isFirstVisit.value === true` 감지
3. **모달 자동 오픈** (EventView / ResultView)
4. 사용자가 "확인" 클릭
5. 모달 닫기 → 참여 폼 또는 결과 조회 폼 표시

**설정 가능 항목**:

| 항목            | 설정값     | 용도             |
| --------------- | ---------- | ---------------- |
| `description`   | 문자열     | 초대 인사말      |
| `eventStart`    | YYYY/MM/DD | 이벤트 시작일    |
| `eventEnd`      | YYYY/MM/DD | 이벤트 종료일    |
| `announceStart` | YYYY/MM/DD | 발표 시작일      |
| `announceEnd`   | YYYY/MM/DD | 발표 종료일      |
| `message`       | 문자열     | 참여 안내 메시지 |

**현재 기본값**:

```typescript
// EventView.vue
eventInfo.value = {
  description: "환영합니다. 전화번호를 입력하시면 로또 번호를 전달 드립니다.",
  eventStart: "2025/02/01",
  eventEnd: "2025/03/31",
  announceStart: "2025/04/01",
  announceEnd: "2025/04/15",
  message: "기간안에 많은 참여 부탁드려요~!",
};

// ResultView.vue
eventInfo.value = {
  description:
    "환영합니다. 전화번호를 입력하시면 로또 당첨 결과를 확인해드립니다.",
  eventStart: "2025/02/01",
  eventEnd: "2025/03/31",
  announceStart: "2025/04/01",
  announceEnd: "2025/04/15",
  message: "당첨 결과는 발표 기간에만 확인 가능합니다.",
};
```

**향후 개선안**:

- 백엔드 API에서 동적으로 이벤트 정보 조회
- 다국어 지원 추가
- 모달 노출 횟수 제한 옵션`
