package com.otr.lotto.support;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.otr.lotto.common.CurrentDateProvider;

@TestConfiguration
public class TestDateConfig {
    private static final AtomicReference<LocalDate> FIXED_DATE =
        new AtomicReference<>(LocalDate.of(2025, 2, 15));

    public static void setFixedDate(LocalDate date) {
        FIXED_DATE.set(date);
    }

    @Bean
    @Primary
    public CurrentDateProvider currentDateProvider() {
        return new CurrentDateProvider() {
            @Override
            public LocalDate today() {
                return FIXED_DATE.get();
            }
        };
    }
}
