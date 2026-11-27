package com.hyeon.enjoyyourtrip.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.hyeon.enjoyyourtrip.domain.member.repository.MemberRepository;
import com.hyeon.enjoyyourtrip.domain.member.service.IdentityProviderClient;
import com.jayway.jsonpath.JsonPath;

/**
 * API 통합 테스트 공통 부모. 실제 MySQL(enjoytrip_test)과 Redis(1번 DB)를 쓰며, 각 테스트는 트랜잭션 안에서 실행된 뒤 롤백된다.
 * Keycloak은 띄우지 않는다: JwtDecoder를 가짜(KeycloakTokens)로 바꿔 Keycloak 모양의 토큰을 쓰고,
 * Admin API 클라이언트(IdentityProviderClient)도 가짜로 바꾼다. 보안 필터·역할 변환·회원 등록은 실제 코드가 돈다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MemberRepository memberRepository;

    @MockitoBean
    protected JwtDecoder jwtDecoder;

    @MockitoBean
    protected IdentityProviderClient identityProviderClient;

    protected final KeycloakTokens keycloak = new KeycloakTokens();

    @BeforeEach
    void installFakeKeycloak() {
        keycloak.install(jwtDecoder);
    }

    /** Keycloak에서 가입하고 처음 로그인(GET /members/me)한 것과 같다. 회원 id를 돌려준다. */
    protected long signup(String name, String email) throws Exception {
        keycloak.register(email, name, "USER", "offline_access");
        return firstLogin(email);
    }

    /** ADMIN 역할을 가진 Keycloak 사용자로 처음 로그인한다. */
    protected long createAdmin(String email) throws Exception {
        keycloak.register(email, "관리자", "USER", "ADMIN");
        return firstLogin(email);
    }

    protected Tokens login(String email) {
        return new Tokens(keycloak.issue(email));
    }

    protected static String bearer(Tokens tokens) {
        return "Bearer " + tokens.accessToken();
    }

    private long firstLogin(String email) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(email))))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
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

    public record Tokens(String accessToken) {
    }
}
