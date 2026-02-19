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
}
