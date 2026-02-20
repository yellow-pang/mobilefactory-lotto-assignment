package com.otr.lotto.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.otr.lotto.domain.TicketPool;

/**
 * 로또 번호 풀 Mapper
 * 
 * ticket_pool 테이블에 대한 데이터 접근 계층
 */
@Mapper
public interface TicketPoolMapper {
    /**
     * 번호 풀 대량 삽입
     * 
     * @param pools 삽입할 TicketPool 리스트 (최대 10,000개)
     * @return 삽입한 행의 수
     */
    int insertBatch(@Param("pools") List<TicketPool> pools);

    /**
     * 이벤트별 생성된 번호 풀 개수 조회
     * 
     * @param eventId 이벤트 ID
     * @return 생성된 항목 수
     */
    long countByEvent(@Param("eventId") Long eventId);

    /**
     * seq를 기준으로 번호 풀 조회
     * 
     * @param eventId 이벤트 ID
     * @param seq 참여순번 (=participant.id)
     * @return TicketPool 엔티티 또는 null
     */
    TicketPool findByEventAndSeq(@Param("eventId") Long eventId, @Param("seq") Long seq);

    /**
     * 특정 rank의 미배정 번호 풀 조회
     * 
     * 1등 보장이나 순위 스왑 시 사용
     * 
     * @param eventId 이벤트 ID
     * @param rank 찾을 순위 (0/1/2/3/4)
     * @return 미배정 TicketPool 항목 (assignedParticipantId = null)
     */
    TicketPool findUnassignedByRank(@Param("eventId") Long eventId, @Param("rank") Integer rank);

    /**
     * 번호 풀 항목의 rank와 번호 업데이트
     * 
     * @param id TicketPool PK
     * @param rank 변경할 순위
     * @param lottoNumber 변경할 번호 (CSV 형식)
     * @return 업데이트한 행의 수
     */
    int updateRankAndNumber(
        @Param("id") Long id,
        @Param("rank") Integer rank,
        @Param("lottoNumber") String lottoNumber
    );

    /**
     * 번호 풀에 참여자 배정
     * 
     * @param id TicketPool PK
     * @param participantId 배정할 참여자 ID
     * @return 업데이트한 행의 수
     */
    int assignParticipant(@Param("id") Long id, @Param("participantId") Long participantId);

    /**
     * 배정된 당첨자 조회
     * 
     * 당첨 산정 시 호출하여 rank >= 1인 항목 조회
     * 
     * @param eventId 이벤트 ID
     * @return 배정된 당첨자 TicketPool 리스트 (rank 1~4)
     */
    List<TicketPool> findAssignedWinners(@Param("eventId") Long eventId);
}
