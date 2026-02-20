package com.otr.lotto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로또 이벤트 참여 응답
 * 
 * API 식접 늨 참여순번과 배정된 로또 번호를 반환합니다.
 * 
 * 분른 스페싁:
 * - participantId: 참여순번 (= participant.id = ticket_pool.seq)
 * - lottoNumber: CSV 형식 (예: "3,11,22,33,41,45")
 * 
 * SMS 발송 연동:
 * - 참여 중비쉬 시첼저동요’ 메시지를 햸대씼 SMS로 발송 가능
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipateResponse {
    private Long participantId;
    private String lottoNumber;
}
