package com.otr.lotto.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.otr.lotto.domain.Event;

@Mapper
public interface EventMapper {
    Event findById(@Param("id") Long id);
}
