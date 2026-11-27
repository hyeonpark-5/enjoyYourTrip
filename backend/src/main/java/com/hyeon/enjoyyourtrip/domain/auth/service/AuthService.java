package com.hyeon.enjoyyourtrip.domain.auth.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hyeon.enjoyyourtrip.domain.auth.dto.Request.LoginRequest;
import com.hyeon.enjoyyourtrip.domain.auth.dto.Request.PasswordResetRequest;
import com.hyeon.enjoyyourtrip.domain.auth.dto.Request.SignupRequest;
import com.hyeon.enjoyyourtrip.domain.auth.exception.InvalidCredentialsException;
import com.hyeon.enjoyyourtrip.domain.auth.exception.InvalidRefreshTokenException;
import com.hyeon.enjoyyourtrip.domain.auth.exception.MemberInfoMismatchException;
import com.hyeon.enjoyyourtrip.domain.auth.mapper.AuthMapper;
import com.hyeon.enjoyyourtrip.domain.auth.repository.RefreshTokenRepository;
import com.hyeon.enjoyyourtrip.domain.member.dto.Response.MemberResponse;
import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.domain.member.exception.DuplicateEmailException;
import com.hyeon.enjoyyourtrip.domain.member.mapper.MemberMapper;
import com.hyeon.enjoyyourtrip.domain.member.repository.MemberRepository;
import com.hyeon.enjoyyourtrip.global.security.AccessTokenBlacklist;
import com.hyeon.enjoyyourtrip.global.security.JwtProperties;
import com.hyeon.enjoyyourtrip.global.security.JwtTokenProvider;
import com.hyeon.enjoyyourtrip.global.security.JwtTokenProvider.IssuedAccessToken;

import lombok.RequiredArgsConstructor;

/**
 * 가입·로그인·토큰 재발급·로그아웃·비밀번호 재설정.
 * Refresh Token은 매 재발급마다 새로 바꾼다(rotation) — 탈취된 토큰은 한 번만 쓸 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final MemberRepository memberRepository;
    private final AuthMapper authMapper;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenBlacklist accessTokenBlacklist;
    private final Clock clock;

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        String email = normalize(request.email());
        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        Member member = authMapper.toEntity(request, email, passwordEncoder.encode(request.password()));
        return memberMapper.toResponse(memberRepository.save(member));
    }

    public boolean isEmailAvailable(String email) {
        return !memberRepository.existsByEmail(normalize(email));
    }

    public TokenPair login(LoginRequest request) {
        Member member = memberRepository.findByEmailAndDeletedAtIsNull(normalize(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(InvalidCredentialsException::new);
        return issueTokens(member);
    }

    public TokenPair reissue(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        Long memberId = refreshTokenRepository.findMemberId(refreshToken)
                .orElseThrow(InvalidRefreshTokenException::new);
        refreshTokenRepository.delete(refreshToken);
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(InvalidRefreshTokenException::new);
        return issueTokens(member);
    }

    /**
     * @param accessTokenId        현재 Access Token의 jti — 남은 유효시간 동안 블랙리스트에 올린다.
     * @param accessTokenExpiresAt 현재 Access Token의 만료 시각
     * @param refreshToken         쿠키의 Refresh Token (없으면 null)
     */
    public void logout(String accessTokenId, Instant accessTokenExpiresAt, String refreshToken) {
        if (accessTokenExpiresAt != null) {
            accessTokenBlacklist.add(accessTokenId, Duration.between(clock.instant(), accessTokenExpiresAt));
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.delete(refreshToken);
        }
    }

    /** 재설정 후 모든 기기의 로그인을 끊는다. */
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        Member member = memberRepository.findByEmailAndDeletedAtIsNull(normalize(request.email()))
                .filter(found -> found.getName().equals(request.name().trim()))
                .orElseThrow(MemberInfoMismatchException::new);
        member.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.deleteAllByMemberId(member.getId());
    }

    private TokenPair issueTokens(Member member) {
        IssuedAccessToken accessToken = jwtTokenProvider.issue(member.getId(), member.getRole().name());
        String refreshToken = newRefreshToken();
        refreshTokenRepository.save(refreshToken, member.getId(), jwtProperties.refreshTokenTtl());
        return new TokenPair(accessToken.value(), accessToken.expiresIn(), refreshToken);
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
