package com.otr.lotto.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.otr.lotto.domain.SmsLog;

@Mapper
public interface SmsLogMapper {
    int insert(SmsLog smsLog);
}
