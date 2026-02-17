package com.otr.lotto.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class DummyController {

    @GetMapping("/success")
    public ApiResponse<DummyData> testSuccess() {
        return ApiResponse.success(new DummyData("test", 123));
    }

    @GetMapping("/success-empty")
    public ApiResponse<?> testSuccessEmpty() {
        return ApiResponse.success();
    }

    @GetMapping("/error")
    public ApiResponse<?> testError(@RequestParam String errorCode) {
        ErrorCode code = ErrorCode.valueOf(errorCode);
        throw new ApiException(code);
    }

    @GetMapping("/error-custom-message")
    public ApiResponse<?> testErrorCustomMessage() {
        throw new ApiException(ErrorCode.INVALID_REQUEST, "사용자 정의 에러 메시지입니다.");
    }

    @PostMapping("/validation")
    public ApiResponse<?> testValidation(@Valid @RequestBody DummyRequest request) {
        return ApiResponse.success(request);
    }

    @GetMapping("/internal-error")
    public ApiResponse<?> testInternalError() {
        throw new RuntimeException("Internal server error test");
    }

    @Getter
    public static class DummyData {
        private final String name;
        private final int value;

        public DummyData(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    @Getter
    @Setter
    public static class DummyRequest {
        @NotBlank(message = "이름은 필수입니다.")
        private String name;

        @Positive(message = "나이는 0 이상이어야 합니다.")
        private int age;
    }
}
