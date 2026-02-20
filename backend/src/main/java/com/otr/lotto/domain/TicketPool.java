package com.otr.lotto.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로또 번호 풀 엔티티
 * 
 * 이벤트당 10,000개의 사전 생성된 로또 번호를 저장하는 엔티티입니다.
 * 각 항목은 seq(참여순번)를 기준으로 rank와 번호가 확정되어 있습니다.
 * 
 * 특징:
 * - seq는 participant.id와 1:1 매핑 (자동증가 값이 seq가 됨)
 * - assignedParticipantId는 null → 미배정, null 아님 → 배정 완료
 * - rank는 0(비당첨) ~ 4(4등)
 * - lottoNumber는 CSV 형태 (예: "3,11,22,33,41,45")
 * 
 * 라이프사이클:
 * 1. preparePool() 호출 → 10,000 항목 일괄 생성
 * 2. 참여 시 seq 기반으로 조회 및 배정
 * 3. 당첨 산정 시 readForDraw() 호출하여 당첨자 조회
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketPool {
    /** 테이블 PK */
    private Long id;
    
    /** 이벤트 ID */
    private Long eventId;
    
    /** 참여순번 (=participant.id) */
    private Long seq;
    
    /** 배정된 로또 번호 (CSV 형식) */
    private String lottoNumber;
    
    /** 순위 (0=비당첨, 1=1등, 2=2등, 3=3등, 4=4등) */
    private Integer rank;
    
    /** 배정된 참여자 ID (null이면 미배정) */
    private Long assignedParticipantId;
    
    /** 생성 시간 */
    private LocalDateTime createdAt;
}
