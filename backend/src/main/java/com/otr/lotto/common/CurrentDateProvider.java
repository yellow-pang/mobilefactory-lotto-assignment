package com.otr.lotto.common;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 현재 날짜 제공자
 * 
 * 테스트 환경에서 날짜를 제어할 수 있도록 하기 위한 컴포넌트입니다.
 * 우선순위:
 * 1. application.yaml의 test.current-date 설정
 * 2. TEST_CURRENT_DATE 환경변수
 * 3. LocalDate.now() (운영 환경)
 * 
 * 사용 예:
 * - 프로덕션: 설정 없음 → LocalDate.now() 사용
 * - 개발/테스트: application.yaml에 test.current-date: 2025-02-15 설정
 */
@Component
public class CurrentDateProvider {

    @Value("${test.current-date:#{null}}")
    private String configTestDate;

    /**
     * 현재 날짜를 반환합니다.
     * 
     * test.current-date 설정 또는 TEST_CURRENT_DATE 환경변수가 있으면 해당 날짜를 반환하고,
     * 없으면 LocalDate.now()를 반환합니다.
     * 
     * @return 현재 날짜 (또는 테스트 날짜)
     */
    public LocalDate today() {
        // 1순위: application.yaml 설정
        if (configTestDate != null && !configTestDate.isBlank()) {
            return LocalDate.parse(configTestDate);
        }
        
        // 2순위: 환경변수
        String testDate = System.getenv("TEST_CURRENT_DATE");
        
        if (testDate != null && !testDate.trim().isEmpty()) {
            return LocalDate.parse(testDate);
        }
        
        return LocalDate.now();
    }
}
