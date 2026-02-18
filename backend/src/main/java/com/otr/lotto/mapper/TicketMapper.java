package com.otr.lotto.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.otr.lotto.domain.Ticket;

@Mapper
public interface TicketMapper {
    int insert(Ticket ticket);

    Ticket findByParticipantId(@Param("participantId") Long participantId);
}
