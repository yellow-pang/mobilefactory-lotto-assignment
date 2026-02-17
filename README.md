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

- DB 스키마 작성 및 적용 완료 (event/participant/ticket/prize/sms_log)
- Backend/Frontend 프로젝트 스캐폴드 생성 완료
- ERD 캡처 완료 (docs/erd.png에 저장 예정 또는 저장 완료)
- 백엔드는 환경변수 기반으로 DB 연결 설정을 사용하는 방향으로 정리 중

## Notes

- 문자 발송(SMS/카카오)은 외부 연동 없이 Mock 처리합니다.
  - 번호는 API 응답/화면에 표시
  - 발송 이력은 sms_log 테이블에 기록하는 방식으로 설계합니다.
- 휴대폰 번호 원문은 저장하지 않고 해시(phone_hash)로 저장하는 방향을 사용합니다.
- 과제 제출을 위해 README에 로컬 실행 재현 절차를 우선 정리합니다.
