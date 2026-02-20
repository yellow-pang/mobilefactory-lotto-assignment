package com.otr.lotto.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.otr.lotto.domain.Event;

@Mapper
public interface EventMapper {
    Event findById(@Param("id") Long id);

    /**
     * 기준 날짜 범위 내 활성화된 이벤트 조회
     * @param baseDate 기준 날짜 (보통 LocalDate.now())
     * @return 활성화된 Event (없으면 null)
     */
    Event findActiveEvent(@Param("baseDate") java.time.LocalDate baseDate);

    /**
     * 기준 날짜 범위 내 발표 기간 이벤트 조회
     * @param baseDate 기준 날짜 (보통 LocalDate.now())
     * @return 발표 기간 Event (없으면 null)
     */
    Event findActiveAnnounceEvent(@Param("baseDate") java.time.LocalDate baseDate);

    /**
     * 당첨 번호 업데이트
     */
    int updateWinningNumber(@Param("id") Long id, @Param("winningNumber") String winningNumber);

    /**
     * 미확인자 알림 발송 대상 이벤트 조회
     * (발표 시작일 + 10일 = 목표 날짜인 이벤트)
     * @param targetDate 목표 날짜
     * @return 알림 발송 대상 이벤트 목록
     */
    java.util.List<Event> findEventsReadyForReminder(@Param("targetDate") java.time.LocalDate targetDate);
}
