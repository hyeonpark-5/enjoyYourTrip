package com.hyeon.enjoyyourtrip.global.security;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Access Token(JWT) 발급.
 * 클레임: iss, sub(회원 id), iat, exp, jti(로그아웃 블랙리스트용), roles(["USER"] 형태)
 * Keycloak 도입 시 이 클래스만 제거하면 되고, 검증 쪽(SecurityConfig)은 issuer-uri만 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    public static final String ROLES_CLAIM = "roles";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public IssuedAccessToken issue(Long memberId, String role) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim(ROLES_CLAIM, List.of(role))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, properties.accessTokenTtl().toSeconds());
    }

    /** @param expiresIn 만료까지 남은 초 */
    public record IssuedAccessToken(String value, long expiresIn) {
    }
}
