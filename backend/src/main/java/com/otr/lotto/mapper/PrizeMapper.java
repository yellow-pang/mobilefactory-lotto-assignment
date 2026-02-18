package com.otr.lotto.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.otr.lotto.domain.Prize;

@Mapper
public interface PrizeMapper {
    Prize findByEventAndParticipantId(@Param("eventId") Long eventId, @Param("participantId") Long participantId);
}
