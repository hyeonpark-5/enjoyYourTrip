package com.hyeon.enjoyyourtrip.domain.auth.service;

import java.time.Instant;

import com.hyeon.enjoyyourtrip.domain.auth.dto.Request.LoginRequest;
import com.hyeon.enjoyyourtrip.domain.auth.dto.Request.PasswordResetRequest;
import com.hyeon.enjoyyourtrip.domain.auth.dto.Request.SignupRequest;
import com.hyeon.enjoyyourtrip.domain.member.dto.Response.MemberResponse;

/**
 * 가입·로그인·토큰 재발급·로그아웃·비밀번호 재설정.
 */
public interface AuthService {

    /** USER 권한으로 가입시킨다. 이메일은 소문자로 정규화되며, 탈퇴한 이메일도 중복으로 본다. */
    MemberResponse signup(SignupRequest request);

    boolean isEmailAvailable(String email);

    /** 이메일·비밀번호가 맞으면 Access/Refresh Token을 발급한다. */
    TokenPair login(LoginRequest request);

    /** Refresh Token으로 새 토큰 묶음을 발급하고, 쓴 Refresh Token은 폐기한다(rotation). */
    TokenPair reissue(String refreshToken);

    /**
     * @param accessTokenId        현재 Access Token의 jti — 남은 유효시간 동안 블랙리스트에 올린다.
     * @param accessTokenExpiresAt 현재 Access Token의 만료 시각
     * @param refreshToken         쿠키의 Refresh Token (없으면 null)
     */
    void logout(String accessTokenId, Instant accessTokenExpiresAt, String refreshToken);

    /** 이메일과 이름이 일치하면 새 비밀번호로 바꾸고 모든 기기의 로그인을 끊는다. */
    void resetPassword(PasswordResetRequest request);
}
