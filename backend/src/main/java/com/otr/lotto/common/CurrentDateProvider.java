package com.otr.lotto.common;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

/**
 * 현재 날짜 제공자
 * 
 * 테스트 환경에서 날짜를 제어할 수 있도록 하기 위한 컴포넌트입니다.
 * TEST_CURRENT_DATE 환경변수를 설정하면 해당 날짜를 반환합니다.
 * 환경변수가 없으면 LocalDate.now()를 반환합니다.
 * 
 * 사용 예:
 * - 프로덕션: TEST_CURRENT_DATE를 설정하지 않음 → LocalDate.now() 사용
 * - 테스트: set TEST_CURRENT_DATE=2025-02-15 → 2025-02-15로 고정
 */
@Component
public class CurrentDateProvider {

    /**
     * 현재 날짜를 반환합니다.
     * 
     * TEST_CURRENT_DATE 환경변수가 설정되어 있으면 해당 날짜를 반환하고,
     * 없으면 LocalDate.now()를 반환합니다.
     * 
     * @return 현재 날짜 (또는 테스트 날짜)
     */
    public LocalDate today() {
        String testDate = System.getenv("TEST_CURRENT_DATE");
        
        if (testDate != null && !testDate.trim().isEmpty()) {
            return LocalDate.parse(testDate);
        }
        
        return LocalDate.now();
    }
}
