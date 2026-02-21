package com.otr.lotto.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/test")
public class TestDummyController {

    @GetMapping("/success")
    public ApiResponse<DummyData> testSuccess() {
        return ApiResponse.success(new DummyData("test", 123));
    }

    @GetMapping("/success-empty")
    public ApiResponse<?> testSuccessEmpty() {
        return ApiResponse.success();
    }

    @GetMapping("/error")
    public ApiResponse<?> testError(@RequestParam("errorCode") String errorCode) {
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

    public static class DummyRequest {
        @NotBlank
        private String name;

        @Positive
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

    public static class DummyData {
        private String name;
        private int value;

        public DummyData(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
