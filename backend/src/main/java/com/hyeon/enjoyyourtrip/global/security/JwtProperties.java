package com.hyeon.enjoyyourtrip.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.jwt.* 설정. secret은 .env의 JWT_SECRET에서 읽으며 HS256 서명을 위해 32바이트 이상이어야 한다.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        RefreshCookie refreshCookie) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret(JWT_SECRET)이 비어 있거나 32바이트 미만입니다. backend/.env에 JWT_SECRET을 설정하세요.");
        }
    }

    /**
     * Refresh Token 쿠키 속성. 배포(HTTPS) 환경에서는 secure=true로 둔다.
     */
    public record RefreshCookie(String path, String sameSite, boolean secure) {
    }
}
