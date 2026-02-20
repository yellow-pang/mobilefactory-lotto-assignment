package com.otr.lotto.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로또 이벤트 엔티티
 * 
 * 로또 이벤트의 모든 설정 정보를 저장합니다.
 * 이벤트는 시스템의 최상위 단위로, 모든 참여/당첨 정보는 이벤트와 연결됩니다.
 * 
 * 중요 필드:
 * - eventStart/eventEnd: 참여 기간
 * - announceStart/announceEnd: 당첨 결과 공개 기간
 * - winningNumber: CSV 형태의 당첨 번호 (예: "3,11,22,33,41,45")
 * - fixedFirstPhoneHash: 1등이 확정된 휴대폰 번호의 해시 (null이면 미정)
 * - maxParticipants: 최대 참여자 수 (null이면 10,000)
 * 
 * 운영:
 * - preparePool() 호출 전에 반드시 winningNumber와 fixedFirstPhoneHash 설정
 * - 이벤트 생성 후 수정하면 이미 생성된 ticket_pool에 영향 없음
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private Long id;
    private String name;
    private LocalDate eventStart;
    private LocalDate eventEnd;
    private LocalDate announceStart;
    private LocalDate announceEnd;
    private Integer maxParticipants;
    private String winningNumber;
    private String fixedFirstPhoneHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
