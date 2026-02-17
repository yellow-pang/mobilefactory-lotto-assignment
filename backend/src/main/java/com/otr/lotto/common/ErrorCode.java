package com.otr.lotto.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Client Errors
    INVALID_REQUEST("INVALID_REQUEST", "요청 형식이 잘못되었습니다."),
    EVENT_NOT_ACTIVE("EVENT_NOT_ACTIVE", "이벤트가 활성화되지 않았습니다."),
    ANNOUNCE_NOT_ACTIVE("ANNOUNCE_NOT_ACTIVE", "공지사항이 활성화되지 않았습니다."),
    DUPLICATE_PARTICIPATION("DUPLICATE_PARTICIPATION", "이미 참여하였습니다."),
    CAPACITY_FULL("CAPACITY_FULL", "참여 인원이 가득 찼습니다."),
    NOT_FOUND("NOT_FOUND", "찾을 수 없습니다."),

    // Server Error
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
