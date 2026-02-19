-- schema.sql (MariaDB / InnoDB / utf8mb4)

SET NAMES utf8mb4;
SET time_zone = '+09:00';

-- 기존 테이블을 FK 기준에 맞춰서 삭제
DROP TABLE IF EXISTS sms_log;
DROP TABLE IF EXISTS prize;
DROP TABLE IF EXISTS ticket;
DROP TABLE IF EXISTS participant;
DROP TABLE IF EXISTS event;

-- 1) event
CREATE TABLE event (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  event_start DATE NOT NULL,
  event_end DATE NOT NULL,
  announce_start DATE NOT NULL,
  announce_end DATE NOT NULL,
  max_participants INT NOT NULL DEFAULT 10000,
  winning_number VARCHAR(32) NULL,
  fixed_first_phone_hash CHAR(64) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) participant
-- 참가자 번호(참여순번)는 participant.id를 그대로 사용(이벤트 1개 전제)
CREATE TABLE participant (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  event_id BIGINT UNSIGNED NOT NULL,
  phone_hash CHAR(64) NOT NULL,
  -- 결과 확인 정책(1회차/2회차)을 위해 최소 컬럼만 둡니다.
  check_count INT NOT NULL DEFAULT 0,
  first_checked_at DATETIME NULL,
  last_checked_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_participant_event
    FOREIGN KEY (event_id) REFERENCES event(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT uq_participant_event_phone
    UNIQUE (event_id, phone_hash),
  INDEX idx_participant_event_id (event_id, id),
  INDEX idx_participant_event_created (event_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) ticket
-- 참여 1건당 로또 번호 1개
CREATE TABLE ticket (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  event_id BIGINT UNSIGNED NOT NULL,
  participant_id BIGINT UNSIGNED NOT NULL,
  lotto_number VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_ticket_event
    FOREIGN KEY (event_id) REFERENCES event(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_ticket_participant
    FOREIGN KEY (participant_id) REFERENCES participant(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT uq_ticket_participant
    UNIQUE (participant_id),
  INDEX idx_ticket_event_participant (event_id, participant_id),
  INDEX idx_ticket_event_number (event_id, lotto_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) prize
-- 당첨자만 저장(총 1,000행이 되도록 산정)
CREATE TABLE prize (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  event_id BIGINT UNSIGNED NOT NULL,
  participant_id BIGINT UNSIGNED NOT NULL,
  rank TINYINT UNSIGNED NOT NULL, -- 1/2/3/4
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_prize_event
    FOREIGN KEY (event_id) REFERENCES event(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_prize_participant
    FOREIGN KEY (participant_id) REFERENCES participant(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT uq_prize_event_participant
    UNIQUE (event_id, participant_id),
  INDEX idx_prize_event_rank (event_id, rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5) sms_log
-- 실제 문자 연동 대신 Mock 처리해도, "발송 이력/중복 방지"를 보여주기 위한 테이블
CREATE TABLE sms_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  event_id BIGINT UNSIGNED NOT NULL,
  participant_id BIGINT UNSIGNED NULL,
  phone_hash CHAR(64) NOT NULL,
  type VARCHAR(30) NOT NULL,      -- PARTICIPATION_NUMBER / UNCONFIRMED_WINNER_REMINDER
  sent_date DATE NOT NULL,        -- 중복 방지용(날짜 단위)
  status VARCHAR(20) NOT NULL,    -- SENT / FAILED
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_sms_event
    FOREIGN KEY (event_id) REFERENCES event(id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_sms_participant
    FOREIGN KEY (participant_id) REFERENCES participant(id)
    ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT uq_sms_dedup
    UNIQUE (event_id, phone_hash, type, sent_date),
  INDEX idx_sms_event_type_status (event_id, type, status),
  INDEX idx_sms_event_phone (event_id, phone_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6) 초기 이벤트 데이터 (요구사항 기간)
INSERT INTO event (
  id,
  name,
  event_start,
  event_end,
  announce_start,
  announce_end,
  max_participants,
  winning_number,
  fixed_first_phone_hash,
  created_at,
  updated_at
) VALUES (
  1,
  'Spring Lotto 2025',
  '2025-02-01',
  '2025-03-31',
  '2025-04-01',
  '2025-04-15',
  10000,
  NULL,
  NULL,
  NOW(),
  NOW()
);
