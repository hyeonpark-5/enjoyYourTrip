package com.hyeon.enjoyyourtrip.global.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Refresh Token은 자바스크립트가 읽을 수 없는 HttpOnly 쿠키로만 주고받는다(XSS로 탈취 방지).
 * path를 /api/v1/auth로 제한해 다른 API 호출에는 실리지 않는다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookieProvider {

    public static final String COOKIE_NAME = "refresh_token";

    private final JwtProperties properties;

    public ResponseCookie create(String refreshToken) {
        return build(refreshToken, properties.refreshTokenTtl());
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        JwtProperties.RefreshCookie cookie = properties.refreshCookie();
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookie.secure())
                .sameSite(cookie.sameSite())
                .path(cookie.path())
                .maxAge(maxAge)
                .build();
    }
}
