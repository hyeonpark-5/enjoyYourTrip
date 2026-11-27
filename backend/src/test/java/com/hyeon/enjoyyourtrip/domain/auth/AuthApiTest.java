package com.hyeon.enjoyyourtrip.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.hyeon.enjoyyourtrip.support.ApiTestSupport;

class AuthApiTest extends ApiTestSupport {

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("성공하면 201과 USER 권한 회원 정보를 돌려주고 비밀번호는 노출하지 않는다")
        void success() throws Exception {
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("name", "여행자", "email", "Traveler@Example.com", "password", PASSWORD)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(notNullValue()))
                    .andExpect(jsonPath("$.email").value("traveler@example.com"))
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("대소문자만 다른 이메일도 중복으로 보고 409를 돌려준다")
        void duplicateEmail() throws Exception {
            signup("여행자", "dup@example.com");

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("name", "다른사람", "email", "DUP@example.com", "password", PASSWORD)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 400과 필드 오류를 돌려준다")
        void shortPassword() throws Exception {
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("name", "여행자", "email", "short@example.com", "password", "1234")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.errors[0].field").value("password"));
        }

        @Test
        @DisplayName("이메일 중복 확인은 로그인 없이 쓸 수 있다")
        void emailCheck() throws Exception {
            signup("여행자", "taken@example.com");

            mockMvc.perform(get("/api/v1/auth/email-check").param("email", "taken@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(false));
            mockMvc.perform(get("/api/v1/auth/email-check").param("email", "free@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available").value(true));
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("성공하면 Access Token은 본문, Refresh Token은 HttpOnly 쿠키로 내려간다")
        void success() throws Exception {
            signup("여행자", "login@example.com");

            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("email", "login@example.com", "password", PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(1800))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                    .andReturn();
            Tokens tokens = Tokens.from(result);
            assertThat(tokens.refreshToken()).isNotBlank();

            mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("login@example.com"));
        }

        @Test
        @DisplayName("비밀번호가 틀리거나 없는 이메일이면 같은 401 LOGIN_FAILED를 돌려준다")
        void failure() throws Exception {
            signup("여행자", "fail@example.com");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("email", "fail@example.com", "password", "wrong-password")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("email", "nobody@example.com", "password", PASSWORD)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        @Test
        @DisplayName("Refresh Token으로 새 토큰을 받고, 이미 쓴 Refresh Token은 다시 쓸 수 없다")
        void rotation() throws Exception {
            signup("여행자", "reissue@example.com");
            Tokens first = login("reissue@example.com");

            MvcResult result = mockMvc.perform(post("/api/v1/auth/reissue").cookie(first.refreshCookie()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                    .andReturn();
            Tokens second = Tokens.from(result);
            assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());

            mockMvc.perform(post("/api/v1/auth/reissue").cookie(first.refreshCookie()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
            mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer(second)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("쿠키가 없으면 401을 돌려준다")
        void withoutCookie() throws Exception {
            mockMvc.perform(post("/api/v1/auth/reissue"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("로그아웃하면 쿠키가 지워지고 같은 Access/Refresh Token을 더 쓸 수 없다")
        void logout() throws Exception {
            signup("여행자", "logout@example.com");
            Tokens tokens = login("logout@example.com");

            mockMvc.perform(post("/api/v1/auth/logout")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                            .cookie(tokens.refreshCookie()))
                    .andExpect(status().isNoContent())
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

            mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
            mockMvc.perform(post("/api/v1/auth/reissue").cookie(tokens.refreshCookie()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("토큰 없이 로그아웃하면 401을 돌려준다")
        void withoutToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정")
    class PasswordReset {

        @Test
        @DisplayName("이메일과 이름이 맞으면 새 비밀번호로 바뀌고 기존 로그인은 모두 끊긴다")
        void success() throws Exception {
            signup("여행자", "reset@example.com");
            Tokens before = login("reset@example.com");

            mockMvc.perform(post("/api/v1/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("email", "reset@example.com", "name", "여행자", "newPassword", "newPassword99")))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("email", "reset@example.com", "password", PASSWORD)))
                    .andExpect(status().isUnauthorized());
            login("reset@example.com", "newPassword99");
            mockMvc.perform(post("/api/v1/auth/reissue").cookie(before.refreshCookie()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("이름이 다르면 400 MEMBER_INFO_MISMATCH를 돌려준다")
        void mismatch() throws Exception {
            signup("여행자", "mismatch@example.com");

            mockMvc.perform(post("/api/v1/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("email", "mismatch@example.com", "name", "다른이름", "newPassword", "newPassword99")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MEMBER_INFO_MISMATCH"));
        }
    }
}
