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
}
