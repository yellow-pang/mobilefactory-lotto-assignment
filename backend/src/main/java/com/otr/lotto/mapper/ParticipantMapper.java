package com.otr.lotto.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.otr.lotto.domain.Participant;

@Mapper
public interface ParticipantMapper {
    int insert(Participant participant);

    Participant findByEventAndPhoneHash(@Param("eventId") Long eventId, @Param("phoneHash") String phoneHash);

    long countByEvent(@Param("eventId") Long eventId);

    int updateCheckCountAndTimestamps(
            @Param("eventId") Long eventId,
            @Param("participantId") Long participantId,
            @Param("checkedAt") java.time.LocalDateTime checkedAt
    );

    /**
     * 참여 번호 (ID) 범위로 참여자 조회 (2등, 3등 후보군 선정용)
     */
    List<Participant> findByIdRange(
        @Param("eventId") Long eventId,
        @Param("minId") Long minId,
        @Param("maxId") Long maxId
    );

    /**
     * 특정 이벤트의 모든 참여자 조회 (4등 후보군 선정용)
     */
    List<Participant> findAllByEvent(@Param("eventId") Long eventId);
}
