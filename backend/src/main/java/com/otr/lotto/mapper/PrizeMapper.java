package com.otr.lotto.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.otr.lotto.domain.Prize;

@Mapper
public interface PrizeMapper {
    Prize findByEventAndParticipantId(@Param("eventId") Long eventId, @Param("participantId") Long participantId);

    /**
     * 당첨자 목록 대량 삽입
     */
    int insertBatch(@Param("prizes") List<Prize> prizes);

    /**
     * 특정 이벤트의 당첨자 수 조회 (멱등성 체크용)
     */
    long countByEvent(@Param("eventId") Long eventId);

    /**
     * 미확인 당첨자 조회 (check_count = 0인 당첨자)
     */
    List<Prize> findUnconfirmedWinners(@Param("eventId") Long eventId);
}
