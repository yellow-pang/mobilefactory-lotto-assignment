package com.otr.lotto.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.otr.lotto.domain.TicketPool;

@Mapper
public interface TicketPoolMapper {
    int insertBatch(@Param("pools") List<TicketPool> pools);

    long countByEvent(@Param("eventId") Long eventId);

    TicketPool findByEventAndSeq(@Param("eventId") Long eventId, @Param("seq") Long seq);

    TicketPool findUnassignedByRank(@Param("eventId") Long eventId, @Param("rank") Integer rank);

    int updateRankAndNumber(
        @Param("id") Long id,
        @Param("rank") Integer rank,
        @Param("lottoNumber") String lottoNumber
    );

    int assignParticipant(@Param("id") Long id, @Param("participantId") Long participantId);

    List<TicketPool> findAssignedWinners(@Param("eventId") Long eventId);
}
