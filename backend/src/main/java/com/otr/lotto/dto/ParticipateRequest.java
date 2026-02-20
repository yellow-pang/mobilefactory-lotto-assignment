package com.otr.lotto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로또 이벤트 참여 요청
 * 
 * 력로 1개 필드: 휴대폰 번호
 * 두 차 혁 마면 true
 * 
 * 첀증 규칙:
 * - NotBlank: 10~13자 순 숫자와 하이픈(-) 만 답붕
 * 
 * 예시: 010-1234-5678, 01012345678
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipateRequest {
    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    @Pattern(regexp = "^[0-9-]{10,13}$", message = "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String phone;
}
