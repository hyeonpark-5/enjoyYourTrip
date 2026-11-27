package com.hyeon.enjoyyourtrip.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Constructor;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.domain.member.entity.Role;
import com.hyeon.enjoyyourtrip.domain.member.exception.IdentityProviderUnavailableException;
import com.hyeon.enjoyyourtrip.support.ApiTestSupport;

class MemberApiTest extends ApiTestSupport {

    @Nested
    @DisplayName("Keycloak 사용자 → 회원 연결")
    class Sync {

        @Test
        @DisplayName("처음 로그인하면 토큰의 이메일·이름·역할로 회원이 만들어진다")
        void firstLoginCreatesMember() throws Exception {
            keycloak.register("new@example.com", "새회원", "USER");

            mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer(login("new@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("new@example.com"))
                    .andExpect(jsonPath("$.name").value("새회원"))
                    .andExpect(jsonPath("$.role").value("USER"));
            assertThat(memberRepository.findByKeycloakId(keycloak.user("new@example.com").keycloakId())).isPresent();
        }

        @Test
        @DisplayName("Keycloak 도입 전 같은 이메일로 가입한 회원이 있으면 그 회원과 잇는다")
        void linksLegacyMember() throws Exception {
            Member legacy = memberRepository.save(legacyMember("old@example.com", "예전회원"));
            keycloak.register("old@example.com", "키클록이름", "USER");

            mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer(login("old@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(legacy.getId()))
                    .andExpect(jsonPath("$.name").value("예전회원"));
            assertThat(memberRepository.findById(legacy.getId()).orElseThrow().getKeycloakId())
                    .isEqualTo(keycloak.user("old@example.com").keycloakId());
        }

        @Test
        @DisplayName("Keycloak에서 바뀐 역할이 다음 요청부터 반영된다")
        void syncsRoleFromToken() throws Exception {
            signup("김철수", "kim@example.com");
            keycloak.changeRoles("kim@example.com", "USER", "ADMIN");

            mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer(login("kim@example.com"))))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
            mockMvc.perform(get("/api/v1/members").header(HttpHeaders.AUTHORIZATION, bearer(login("kim@example.com"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("토큰이 없으면 401, 위조된 토큰이면 401 INVALID_TOKEN")
        void unauthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/members/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
            mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer forged"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        }
    }

    @Test
    @DisplayName("회원 목록은 관리자만 볼 수 있고 이름·이메일로 검색된다")
    void searchIsAdminOnly() throws Exception {
        signup("김철수", "kim@example.com");
        signup("이영희", "lee@example.com");
        createAdmin("admin@example.com");

        mockMvc.perform(get("/api/v1/members").header(HttpHeaders.AUTHORIZATION, bearer(login("kim@example.com"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/members")
                        .param("key", "name").param("word", "이영")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("lee@example.com"));
    }

    @Test
    @DisplayName("다른 회원 정보는 관리자만 볼 수 있다")
    void detailIsSelfOrAdmin() throws Exception {
        signup("김철수", "kim@example.com");
        long otherId = signup("이영희", "lee@example.com");
        createAdmin("admin@example.com");

        mockMvc.perform(get("/api/v1/members/{id}", otherId).header(HttpHeaders.AUTHORIZATION, bearer(login("kim@example.com"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/members/{id}", otherId).header(HttpHeaders.AUTHORIZATION, bearer(login("lee@example.com"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/members/{id}", otherId).header(HttpHeaders.AUTHORIZATION, bearer(login("admin@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("이영희"));
    }

    @Test
    @DisplayName("없는 회원을 조회하면 404 MEMBER_NOT_FOUND")
    void notFound() throws Exception {
        createAdmin("admin@example.com");

        mockMvc.perform(get("/api/v1/members/{id}", 987654321L).header(HttpHeaders.AUTHORIZATION, bearer(login("admin@example.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("본인은 화면에 보이는 이름을 바꿀 수 있다")
    void updateName() throws Exception {
        long id = signup("김철수", "kim@example.com");

        mockMvc.perform(patch("/api/v1/members/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("kim@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "김여행")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김여행"));
    }

    @Test
    @DisplayName("권한 변경은 관리자만 할 수 있고 Keycloak 역할도 바꾼다")
    void changeRoleIsAdminOnly() throws Exception {
        long id = signup("김철수", "kim@example.com");
        createAdmin("admin@example.com");

        mockMvc.perform(patch("/api/v1/members/{id}/role", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("kim@example.com")))
                        .contentType(MediaType.APPLICATION_JSON).content(json("role", "ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/members/{id}/role", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login("admin@example.com")))
                        .contentType(MediaType.APPLICATION_JSON).content(json("role", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        verify(identityProviderClient).setAdmin(keycloak.user("kim@example.com").keycloakId(), true);
    }

    @Test
    @DisplayName("탈퇴하면 Keycloak 계정이 비활성화되고, 남은 토큰으로는 더 쓸 수 없다")
    void withdrawSelf() throws Exception {
        long id = signup("김철수", "bye@example.com");
        Tokens tokens = login("bye@example.com");

        mockMvc.perform(delete("/api/v1/members/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isNoContent());
        verify(identityProviderClient).disableUser(keycloak.user("bye@example.com").keycloakId());

        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_WITHDRAWN"));
        // Keycloak에 같은 이메일로 새 계정을 만들어도 탈퇴한 이메일은 다시 쓸 수 없다.
        keycloak.register("bye@example.com", "다시가입", "USER");
        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer(login("bye@example.com"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_WITHDRAWN"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 요청마다 실제로 커밋·롤백되는지 보기 위해 테스트 트랜잭션 없이 실행
    @DisplayName("Keycloak 호출이 실패하면 탈퇴가 취소되고 502를 돌려준다")
    void withdrawRollsBackWhenKeycloakFails() throws Exception {
        String email = "rollback-" + UUID.randomUUID() + "@example.com";
        long id = signup("김철수", email);
        try {
            willThrow(new IdentityProviderUnavailableException("사용자 비활성화 실패", null))
                    .given(identityProviderClient).disableUser(keycloak.user(email).keycloakId());

            mockMvc.perform(delete("/api/v1/members/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(login(email))))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("IDENTITY_PROVIDER_UNAVAILABLE"));
            mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, bearer(login(email))))
                    .andExpect(status().isOk());
        } finally {
            memberRepository.deleteById(id);
        }
    }

    /** Keycloak 도입 전 방식으로 가입한 회원 (keycloak_id 없음) */
    private static Member legacyMember(String email, String name) throws Exception {
        Constructor<Member> constructor = Member.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Member member = constructor.newInstance();
        ReflectionTestUtils.setField(member, "email", email);
        ReflectionTestUtils.setField(member, "name", name);
        ReflectionTestUtils.setField(member, "role", Role.USER);
        return member;
    }
}
