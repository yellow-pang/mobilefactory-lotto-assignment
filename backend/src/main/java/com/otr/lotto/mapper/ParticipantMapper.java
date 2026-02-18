package com.otr.lotto.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.otr.lotto.domain.Participant;

@Mapper
public interface ParticipantMapper {
    int insert(Participant participant);

    Participant findByEventAndPhoneHash(@Param("eventId") Long eventId, @Param("phoneHash") String phoneHash);

    long countByEvent(@Param("eventId") Long eventId);
}
