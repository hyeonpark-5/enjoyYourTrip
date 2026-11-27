package com.hyeon.enjoyyourtrip.support;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * 테스트용 가짜 Keycloak. 사용자를 등록하고 토큰 문자열을 발급하면, 가짜 JwtDecoder가 그 문자열을
 * Keycloak과 같은 모양의 Jwt(sub, email, name, realm_access.roles)로 돌려준다. 모르는 토큰은 위조로 본다.
 */
public class KeycloakTokens {

    public record User(String keycloakId, String email, String name, List<String> roles) {
    }

    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();
    private final Map<String, User> usersByToken = new ConcurrentHashMap<>();

    /** JwtDecoder 목(mock)이 이 레지스트리로 토큰을 해석하게 한다. */
    public void install(JwtDecoder decoder) {
        given(decoder.decode(anyString())).willAnswer(invocation -> {
            String token = invocation.getArgument(0);
            User user = usersByToken.get(token);
            if (user == null) {
                throw new BadJwtException("알 수 없는 토큰");
            }
            return toJwt(token, user);
        });
    }

    public User register(String email, String name, String... roles) {
        User user = new User(UUID.randomUUID().toString(), email, name, List.of(roles));
        usersByEmail.put(email, user);
        return user;
    }

    /** 같은 Keycloak 사용자의 역할만 바꾼다(관리 콘솔에서 역할을 바꾼 상황). */
    public User changeRoles(String email, String... roles) {
        User old = usersByEmail.get(email);
        User user = new User(old.keycloakId(), old.email(), old.name(), List.of(roles));
        usersByEmail.put(email, user);
        return user;
    }

    public User user(String email) {
        return usersByEmail.get(email);
    }

    /** 새 Access Token 문자열 */
    public String issue(String email) {
        User user = usersByEmail.get(email);
        if (user == null) {
            throw new IllegalStateException("등록되지 않은 사용자: " + email);
        }
        String token = "token-" + UUID.randomUUID();
        usersByToken.put(token, user);
        return token;
    }

    private static Jwt toJwt(String token, User user) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .issuer("http://localhost:0/realms/enjoyyourtrip")
                .subject(user.keycloakId())
                .audience(List.of("enjoyyourtrip-api"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .claim("email", user.email())
                .claim("name", user.name())
                .claim("preferred_username", user.email())
                .claim("realm_access", Map.of("roles", user.roles()))
                .build();
    }
}
