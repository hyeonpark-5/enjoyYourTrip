package com.hyeon.enjoyyourtrip.domain.auth.dto.Response;

/**
 * 로그인·재발급 응답. Refresh Token은 본문이 아닌 HttpOnly 쿠키(refresh_token)로 내려간다.
 *
 * @param expiresIn Access Token 만료까지 남은 초
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn) {
}
