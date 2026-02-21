package com.otr.lotto.bootstrap;

import java.util.List;

import org.springframework.stereotype.Component;

import com.otr.lotto.domain.Event;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.mapper.TicketPoolMapper;
import com.otr.lotto.service.TicketPoolService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ticket Pool 자동 생성 부트스트랩
 * 
 * 애플리케이션 시작 후 자동으로 실행되어:
 * 1. 모든 이벤트 조회
 * 2. ticket_pool이 없는 이벤트 찾기
 * 3. TicketPoolService를 통해 자동 생성
 * 
 * 역할:
 * - 마이그레이션 SQL 실행 후 혹시 빠진 이벤트 확인
 * - 새로운 이벤트가 등록되었는데 pool이 없으면 자동 생성
 * - 사용자가 참여 전 반드시 pool이 준비되도록 보장
 * 
 * 실행 시점:
 * - 애플리케이션 시작 직후 (예: 00:05)
 * - LottoApplication.main() 이후, 모든 Bean 초기화 완료 시점
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketPoolBootstrap {

    private final EventMapper eventMapper;
    private final TicketPoolService ticketPoolService;
    private final TicketPoolMapper ticketPoolMapper;

    /**
     * 애플리케이션 시작 후 자동 호출
     * Spring의 @PostConstruct 라이프사이클 훅
     */
    @PostConstruct
    public void initializeTicketPools() {
        log.info("========== TicketPool 부트스트랩 시작 ==========");

        try {
            // Step 1: 모든 이벤트 조회
            List<Event> allEvents = eventMapper.findAll();

            if (allEvents == null || allEvents.isEmpty()) {
                log.info("등록된 이벤트가 없습니다");
                log.info("========== TicketPool 부트스트랩 완료 ==========");
                return;
            }

            log.info("DB에 등록된 이벤트: {}개", allEvents.size());

            // Step 2: 각 이벤트마다 ticket_pool이 있나 확인
            int createdCount = 0;
            int skippedCount = 0;
            int failedCount = 0;

            for (Event event : allEvents) {
                try {
                    // ticket_pool 개수 확인
                    Long poolCount = ticketPoolMapper.countByEvent(event.getId());

                    if (poolCount == 0) {
                        // ticket_pool이 없으면 자동 생성
                        log.info(
                            "  ├─ 이벤트 ID: {} ({}): ticket_pool 생성 중...",
                            event.getId(),
                            event.getName()
                        );

                        ticketPoolService.preparePool(event.getId());

                        log.info(
                            "  ├─ 생성 완료 ({}, 10,000개 생성됨)",
                            event.getId()
                        );
                        createdCount++;

                    } else {
                        // ⏭️ 이미 있으면 스킵
                        log.info(
                            "  ├─ 이벤트 ID: {} ({}): 이미 준비됨 ({}개)",
                            event.getId(),
                            event.getName(),
                            poolCount
                        );
                        skippedCount++;
                    }

                } catch (Exception e) {
                    log.warn(
                        "  ├─ 이벤트 {} ticket_pool 생성 실패: {}",
                        event.getId(),
                        e.getMessage()
                    );
                    failedCount++;
                }
            }

            // Step 3: 최종 통계 로깅
            log.info("========== TicketPool 부트스트랩 완료 ==========");
            log.info(
                "통계: 생성됨={}개, 스킵됨={}개, 실패={}개",
                createdCount,
                skippedCount,
                failedCount
            );

            if (failedCount > 0) {
                log.warn("{}개 이벤트의 ticket_pool 생성 실패", failedCount);
            }

            if (createdCount > 0) {
                log.info("{}개 이벤트의 ticket_pool이 자동으로 준비되었습니다", createdCount);
            }

        } catch (Exception e) {
            // 최상위 예외: 부트스트랩 실패해도 앱 시작은 계속
            log.error("TicketPool 부트스트랩 중 예상치 못한 오류 발생", e);
            log.warn("앱이 계속 시작되지만, ticket_pool이 없을 수 있습니다");
            // 주의: 이 경우 수동으로 API 호출 필요
            // POST /api/admin/events/{eventId}/prepare-tickets
        }
    }
}
