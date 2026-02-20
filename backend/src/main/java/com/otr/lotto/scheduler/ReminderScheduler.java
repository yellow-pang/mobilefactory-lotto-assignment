package com.otr.lotto.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.otr.lotto.common.ApiException;
import com.otr.lotto.domain.Event;
import com.otr.lotto.dto.ReminderResponse;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.service.ReminderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 미확인 당첨자 알림 발송 스케줄러
 * 
 * 매일 자정(00:00)에 실행되어:
 * 1. 발표 시작일 + 10일이 오늘인 이벤트 찾기
 * 2. 해당 이벤트의 미확인 당첨자에게 알림 발송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final ReminderService reminderService;
    private final EventMapper eventMapper;

    /**
     * 매일 자정(00:00:00)에 실행
     * 발표 10일 경과한 미확인 당첨자에게 알림 발송
     * 
     * Cron 표현식: "초 분 시 일 월 요일"
     * "0 0 0 * * ?" = 매일 자정
     * 
     * @Transactional: 데이터베이스 일관성 보장
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void sendDailyReminders() {
        log.info("=== 미확인 당첨자 알림 스케줄 시작 ({})", LocalDate.now());

        try {
            // 1. 발표 시작일 + 10일 = 오늘인 이벤트 조회
            LocalDate today = LocalDate.now();
            List<Event> targetEvents = eventMapper.findEventsReadyForReminder(today);

            if (targetEvents == null || targetEvents.isEmpty()) {
                log.info("알림 발송 대상 이벤트 없음");
                return;
            }

            log.info("알림 발송 대상 이벤트 {}개 발견", targetEvents.size());

            // 2. 각 이벤트별로 미확인 당첨자 알림 발송
            int totalSentCount = 0;
            int totalFailedCount = 0;

            for (Event event : targetEvents) {
                try {
                    log.info("  ├─ 이벤트 ID: {}, 이름: {}", event.getId(), event.getName());

                    ReminderResponse result = reminderService.sendUnconfirmedWinnerReminders(event.getId());

                    log.info(
                        "  ├─ 발송 완료: {}명 발송, {}명 실패",
                        result.getRemindersSent(),
                        result.getRemindersFailed()
                    );

                    totalSentCount += result.getRemindersSent();
                    totalFailedCount += result.getRemindersFailed();

                } catch (ApiException e) {
                    log.warn("  ├─ 이벤트 {} 알림 발송 실패: {}", event.getId(), e.getMessage());
                    totalFailedCount++;
                } catch (Exception e) {
                    log.error("  ├─ 예상치 못한 오류 (이벤트 {}): {}", event.getId(), e.getMessage(), e);
                    totalFailedCount++;
                }
            }

            log.info("🔔 === 미확인 당첨자 알림 스케줄 완료");
            log.info(
                "통계: 총 {}명 발송, {}명 실패",
                totalSentCount,
                totalFailedCount
            );

        } catch (Exception e) {
            log.error("미확인 당첨자 알림 스케줄 중 예상치 못한 오류 발생", e);
        }
    }
}
