package com.hyeon.enjoyyourtrip.domain.member;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.hyeon.enjoyyourtrip.support.ApiTestSupport;

class MemberApiTest extends ApiTestSupport {

    @Test
    @DisplayName("회원 목록은 관리자만 볼 수 있고 이름·이메일로 검색된다")
    void searchIsAdminOnly() throws Exception {
        signup("김철수", "kim@example.com");
        signup("이영희", "lee@example.com");
        createAdmin("admin@example.com");
        Tokens user = login("kim@example.com");
        Tokens admin = login("admin@example.com");

        mockMvc.perform(get("/api/v1/members").header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/members")
                        .param("key", "name").param("word", "이영")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("lee@example.com"))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @DisplayName("잘못된 검색 항목은 400을 돌려준다")
    void invalidSearchKey() throws Exception {
        createAdmin("admin@example.com");
        Tokens admin = login("admin@example.com");

        mockMvc.perform(get("/api/v1/members").param("key", "password").param("word", "x")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 회원 정보는 관리자만 볼 수 있다")
    void detailIsSelfOrAdmin() throws Exception {
        signup("김철수", "kim@example.com");
        long otherId = signup("이영희", "lee@example.com");
        createAdmin("admin@example.com");

        mockMvc.perform(get("/api/v1/members/{id}", otherId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("kim@example.com"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/members/{id}", otherId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("lee@example.com"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/members/{id}", otherId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("이영희"));
    }

    @Test
    @DisplayName("없는 회원을 조회하면 404 MEMBER_NOT_FOUND를 돌려준다")
    void notFound() throws Exception {
        createAdmin("admin@example.com");

        mockMvc.perform(get("/api/v1/members/{id}", 987654321L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin@example.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("본인은 이름과 비밀번호를 바꿀 수 있다")
    void updateSelf() throws Exception {
        long id = signup("김철수", "kim@example.com");
        Tokens tokens = login("kim@example.com");

        mockMvc.perform(patch("/api/v1/members/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "김여행", "password", "changedPass1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김여행"));

        login("kim@example.com", "changedPass1");
    }

    @Test
    @DisplayName("권한 변경은 관리자만 할 수 있다")
    void changeRoleIsAdminOnly() throws Exception {
        long id = signup("김철수", "kim@example.com");
        createAdmin("admin@example.com");

        mockMvc.perform(patch("/api/v1/members/{id}/role", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("kim@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("role", "ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/members/{id}/role", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("role", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("본인이 탈퇴하면 토큰이 무효화되고 다시 로그인하거나 같은 이메일로 가입할 수 없다")
    void withdrawSelf() throws Exception {
        long id = signup("김철수", "bye@example.com");
        Tokens tokens = login("bye@example.com");

        mockMvc.perform(delete("/api/v1/members/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .cookie(tokens.refreshCookie()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "bye@example.com", "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "김철수", "email", "bye@example.com", "password", PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("관리자가 탈퇴시키면 그 회원의 Refresh Token이 모두 폐기된다")
    void withdrawByAdmin() throws Exception {
        long id = signup("김철수", "kick@example.com");
        Tokens target = login("kick@example.com");
        createAdmin("admin@example.com");

        mockMvc.perform(delete("/api/v1/members/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin@example.com"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/reissue").cookie(target.refreshCookie()))
                .andExpect(status().isUnauthorized());
    }
}
