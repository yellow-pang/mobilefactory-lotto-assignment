package com.otr.lotto.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.otr.lotto.domain.Ticket;

@Mapper
public interface TicketMapper {
    int insert(Ticket ticket);

    Ticket findByParticipantId(@Param("participantId") Long participantId);

    /**
     * 여러 참여자의 티켓 조회 (당첨 산정용)
     */
    List<Ticket> findByParticipantIds(@Param("participantIds") List<Long> participantIds);

    /**
     * 참여자 번호 범위로 티켓 조회 (번호 발급 제약용)
     */
    List<Ticket> findByParticipantIdRange(
        @Param("eventId") Long eventId,
        @Param("startId") Long startId,
        @Param("endId") Long endId
    );

    /**
     * 이벤트 전체 티켓 조회 (번호 발급 제약용)
     */
    List<Ticket> findByEventId(@Param("eventId") Long eventId);
}
