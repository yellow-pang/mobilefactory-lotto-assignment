package com.otr.lotto.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로또 번호 배정 기록
 * 
 * 참여자가 배정받은 로또 번호를 기록합니다.
 * 
 * 클로
 * - Participant와 1:1 매핑
 * - lottoNumber는 ticket_pool.lottoNumber를 복사한 기록
 * - 결과 조회 시 ticket_pool이 아닌 이 테이블에서 번호 조회 가능
 * 
 * 라이프사이클:
 * 1. ParticipationServiceImpl에서 assignLottoNumber() 실행
 * 2. ticket_pool 조회 후 스왑 처리
 * 3. Ticket 레코드 생성 (번호 기록)
 * 4. 결과 조회 시 이 번호 사용
 * 
 * 참고:
 * - ticket_pool.lottoNumber와 동일해야 함 (감시 필요)
 * - 데이터 정합성: ticket_pool과 sync 유지 필수
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
    private Long id;
    private Long eventId;
    private Long participantId;
    private String lottoNumber;
    private LocalDateTime createdAt;
}
