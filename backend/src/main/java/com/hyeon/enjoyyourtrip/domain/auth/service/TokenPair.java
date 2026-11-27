package com.hyeon.enjoyyourtrip.domain.auth.service;

/**
 * 서비스가 발급한 토큰 묶음. 컨트롤러가 accessToken은 본문(TokenResponse)으로, refreshToken은 쿠키로 나눠 내려준다.
 */
public record TokenPair(
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken) {
}
