package com.hyeon.enjoyyourtrip.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.domain.member.entity.Role;
import com.hyeon.enjoyyourtrip.domain.member.repository.MemberRepository;
import com.hyeon.enjoyyourtrip.global.security.RefreshTokenCookieProvider;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/**
 * API 통합 테스트 공통 부모. 실제 MySQL(enjoytrip_test)과 Redis(1번 DB)를 쓰며,
 * 각 테스트는 트랜잭션 안에서 실행된 뒤 롤백된다(Redis에 남는 토큰은 TTL로 사라짐).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "password1234";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /** 가입 후 회원 id를 돌려준다. */
    protected long signup(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", name, "email", email, "password", PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    protected long createAdmin(String email) {
        return memberRepository.save(
                Member.create(email, passwordEncoder.encode(PASSWORD), "관리자", Role.ADMIN)).getId();
    }

    protected Tokens login(String email) throws Exception {
        return login(email, PASSWORD);
    }

    protected Tokens login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", password)))
                .andExpect(status().isOk())
                .andReturn();
        return Tokens.from(result);
    }

    protected static String bearer(Tokens tokens) {
        return "Bearer " + tokens.accessToken();
    }

    /** 키·값을 번갈아 받아 간단한 JSON 객체 문자열을 만든다. (값은 모두 문자열) */
    protected static String json(String... keyValues) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(keyValues[i]).append("\":\"").append(keyValues[i + 1]).append('"');
        }
        return sb.append('}').toString();
    }

    public record Tokens(String accessToken, String refreshToken, String setCookieHeader) {

        public static Tokens from(MvcResult result) throws Exception {
            String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
            Cookie cookie = result.getResponse().getCookie(RefreshTokenCookieProvider.COOKIE_NAME);
            return new Tokens(accessToken, cookie == null ? null : cookie.getValue(),
                    result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        }

        public Cookie refreshCookie() {
            return new Cookie(RefreshTokenCookieProvider.COOKIE_NAME, refreshToken);
        }
    }
}
