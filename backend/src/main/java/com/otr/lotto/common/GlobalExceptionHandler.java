package com.otr.lotto.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ApiException 처리 (비즈니스 로직 예외)
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<?>> handleApiException(ApiException ex) {
        log.warn("ApiException occurred: code={}, message={}", ex.getCode(), ex.getMessage());
        ApiResponse<?> response = ApiResponse.error(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(getHttpStatus(ex.getErrorCode())).body(response);
    }

    /**
     * 유효성 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("유효성 검증에 실패했습니다.");

        log.warn("Validation failed: {}", message);
        ApiResponse<?> response = ApiResponse.error(
                ErrorCode.INVALID_REQUEST.getCode(),
                message
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 그 외 예외 처리 (Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception ex) {
        log.error("Unexpected exception occurred", ex);
        ApiResponse<?> response = ApiResponse.error(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * ErrorCode에 따른 HTTP Status 결정
     */
    private HttpStatus getHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_REQUEST, EVENT_NOT_ACTIVE, ANNOUNCE_NOT_ACTIVE,
                 DUPLICATE_PARTICIPATION, CAPACITY_FULL -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
