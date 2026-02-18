package com.otr.lotto.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.otr.lotto.domain.SmsLog;

@Mapper
public interface SmsLogMapper {
    int insert(SmsLog smsLog);

    /**
     * 특정 날짜에 발송된 UNCONFIRMED_WINNER_REMINDER 타입 SMS 조회 (중복 발송 방지)
     */
    List<SmsLog> findExistingReminders(
        @Param("eventId") Long eventId,
        @Param("sentDate") LocalDate sentDate,
        @Param("type") String type
    );
}
