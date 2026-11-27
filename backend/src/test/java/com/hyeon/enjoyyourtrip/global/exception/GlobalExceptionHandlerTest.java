package com.hyeon.enjoyyourtrip.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.hyeon.enjoyyourtrip.global.config.CorsConfig;
import com.hyeon.enjoyyourtrip.global.config.JwtConfig;
import com.hyeon.enjoyyourtrip.global.config.SecurityConfig;
import com.hyeon.enjoyyourtrip.global.config.TimeConfig;
import com.hyeon.enjoyyourtrip.global.security.AccessTokenBlacklist;
import com.hyeon.enjoyyourtrip.global.security.ProblemDetailAccessDeniedHandler;
import com.hyeon.enjoyyourtrip.global.security.ProblemDetailAuthenticationEntryPoint;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@ActiveProfiles("test")
@Import({
        GlobalExceptionHandlerTest.TestController.class,
        GlobalExceptionHandlerTest.FixedClockConfig.class,
        SecurityConfig.class,
        CorsConfig.class,
        JwtConfig.class,
        ProblemDetailAuthenticationEntryPoint.class,
        ProblemDetailAccessDeniedHandler.class
})
class GlobalExceptionHandlerTest {

    static final String FIXED_TIME = "2026-11-27T12:00:00+09:00";

    @Autowired
    MockMvc mockMvc;

    /** 웹 슬라이스 테스트에는 Redis가 없으므로 블랙리스트는 가짜로 둔다. */
    @MockitoBean
    AccessTokenBlacklist accessTokenBlacklist;

    @Test
    @DisplayName("위조된 토큰은 401 INVALID_TOKEN으로 응답한다")
    void invalidToken() throws Exception {
        mockMvc.perform(get("/test/not-found").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @WithMockUser
    @DisplayName("도메인 NotFound 예외는 404와 도메인 코드로 응답한다")
    void notFound() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("샘플을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.code").value("SAMPLE_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_TIME));
    }

    @Test
    @WithMockUser
    @DisplayName("도메인 Conflict 예외는 409로 응답한다")
    void conflict() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SAMPLE_DUPLICATE"));
    }

    @Test
    @WithMockUser
    @DisplayName("@Valid 검증 실패는 400과 필드별 오류 목록으로 응답한다")
    void validation() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("이름은 필수입니다."))
                .andExpect(jsonPath("$.instance").value("/test/validate"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_TIME));
    }

    @Test
    @WithMockUser
    @DisplayName("JSON 형식이 깨진 요청은 400과 기본 코드로 응답한다")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").value(FIXED_TIME));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("권한이 부족하면 403으로 응답한다")
    void forbidden() throws Exception {
        mockMvc.perform(get("/test/admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("로그인하지 않으면 401로 응답한다")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser
    @DisplayName("예상하지 못한 예외는 500으로 응답하고 내부 메시지를 노출하지 않는다")
    void unexpected() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다."));
    }

    // ---- 테스트용 컨트롤러와 예외 ----

    record SampleRequest(@NotBlank(message = "이름은 필수입니다.") String name) {
    }

    static class SampleNotFoundException extends NotFoundException {
        SampleNotFoundException() {
            super("SAMPLE_NOT_FOUND", "샘플을 찾을 수 없습니다.");
        }
    }

    static class SampleDuplicateException extends ConflictException {
        SampleDuplicateException() {
            super("SAMPLE_DUPLICATE", "이미 존재하는 샘플입니다.");
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        String notFound() {
            throw new SampleNotFoundException();
        }

        @GetMapping("/test/conflict")
        String conflict() {
            throw new SampleDuplicateException();
        }

        @PostMapping("/test/validate")
        String validate(@Valid @RequestBody SampleRequest request) {
            return request.name();
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/test/admin")
        String admin() {
            return "ok";
        }

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("DB 비밀번호가 틀렸습니다 같은 내부 정보");
        }
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-27T03:00:00Z"), TimeConfig.ZONE);
        }
    }
}
