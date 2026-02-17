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
