package com.otr.lotto.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로또 이벤트 참여자 엔티티
 * 
 * 로또 이벤트에 참여한 사용자의 기본 정보를 저장합니다.
 * 
 * 중요 규칙:
 * - id는 자동증가이며, 같은 값이 ticket_pool.seq가 됨
 * - phoneHash는 휴대폰 번호 정규화(숫자만) 후 SHA256 해시
 * - 이벤트 내에서 phoneHash는 UNIQUE (중복 참여 방지)
 * - checkCount는 당첨 결과 조회 횟수
 * 
 * 보안:
 * - 실제 휴대폰 번호는 저장하지 않고 해시만 저장
 * - 1등 고정 번호 매칭 시에도 해시 값 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Participant {
    /** 테이블 PK (=ticket_pool.seq) */
    private Long id;
    
    /** 이벤트 ID */
    private Long eventId;
    
    /** 휴대폰 번호 SHA256 해시 */
    private String phoneHash;
    
    /** 당첨 결과 조회 횟수 */
    private Integer checkCount;
    
    /** 첫 조회 시간 */
    private LocalDateTime firstCheckedAt;
    
    /** 마지막 조회 시간 */
    private LocalDateTime lastCheckedAt;
    
    /** 생성 시간 */
    private LocalDateTime createdAt;
    
    /** 수정 시간 */
    private LocalDateTime updatedAt;
}
