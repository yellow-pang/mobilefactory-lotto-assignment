package com.otr.lotto.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = { GlobalExceptionHandlerTest.TestConfig.class, TestDummyController.class, GlobalExceptionHandler.class })
@AutoConfigureMockMvc
@DisplayName("GlobalExceptionHandler와 ApiResponse 응답 포맷 테스트")
class GlobalExceptionHandlerTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("성공 응답 - data 포함")
    void testSuccessResponse() throws Exception {
        mockMvc.perform(get("/api/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("test"))
                .andExpect(jsonPath("$.data.value").value(123))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("성공 응답 - 빈 data")
    void testSuccessResponseEmpty() throws Exception {
        mockMvc.perform(get("/api/test/success-empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("ApiException - INVALID_REQUEST")
    void testApiExceptionInvalidRequest() throws Exception {
        mockMvc.perform(get("/api/test/error?errorCode=INVALID_REQUEST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("요청 형식이 잘못되었습니다."));
    }

    @Test
    @DisplayName("ApiException - NOT_FOUND")
    void testApiExceptionNotFound() throws Exception {
        mockMvc.perform(get("/api/test/error?errorCode=NOT_FOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("찾을 수 없습니다."));
    }

    @Test
    @DisplayName("ApiException - 커스텀 메시지")
    void testApiExceptionCustomMessage() throws Exception {
        mockMvc.perform(get("/api/test/error-custom-message"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("사용자 정의 에러 메시지입니다."));
    }

    @Test
    @DisplayName("유효성 검증 실패 - MethodArgumentNotValidException")
    void testValidationException() throws Exception {
        String invalidJson = "{}"; // name, age 필드가 없는 빈 JSON

        mockMvc.perform(post("/api/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("일반 예외 - Internal Server Error")
    void testInternalServerError() throws Exception {
        mockMvc.perform(get("/api/test/internal-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("EVENT_NOT_ACTIVE 에러")
    void testEventNotActive() throws Exception {
        mockMvc.perform(get("/api/test/error?errorCode=EVENT_NOT_ACTIVE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EVENT_NOT_ACTIVE"))
                .andExpect(jsonPath("$.error.message").value("이벤트가 활성화되지 않았습니다."));
    }

    @Test
    @DisplayName("DUPLICATE_PARTICIPATION 에러")
    void testDuplicateParticipation() throws Exception {
        mockMvc.perform(get("/api/test/error?errorCode=DUPLICATE_PARTICIPATION"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_PARTICIPATION"))
                .andExpect(jsonPath("$.error.message").value("이미 참여하였습니다."));
    }
}
