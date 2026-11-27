package com.hyeon.enjoyyourtrip.domain.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hyeon.enjoyyourtrip.domain.auth.dto.Request.LoginRequest;
import com.hyeon.enjoyyourtrip.domain.auth.dto.Request.PasswordResetRequest;
import com.hyeon.enjoyyourtrip.domain.auth.dto.Request.SignupRequest;
import com.hyeon.enjoyyourtrip.domain.auth.dto.Response.EmailCheckResponse;
import com.hyeon.enjoyyourtrip.domain.auth.dto.Response.TokenResponse;
import com.hyeon.enjoyyourtrip.domain.auth.mapper.AuthMapper;
import com.hyeon.enjoyyourtrip.domain.auth.service.AuthService;
import com.hyeon.enjoyyourtrip.domain.auth.service.TokenPair;
import com.hyeon.enjoyyourtrip.domain.member.dto.Response.MemberResponse;
import com.hyeon.enjoyyourtrip.global.security.RefreshTokenCookieProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "회원가입 · 로그인 · 토큰")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthMapper authMapper;
    private final RefreshTokenCookieProvider cookieProvider;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @Operation(summary = "이메일 중복 확인")
    @GetMapping("/email-check")
    public EmailCheckResponse emailCheck(@RequestParam @NotBlank @Email String email) {
        return new EmailCheckResponse(authService.isEmailAvailable(email));
    }

    @Operation(summary = "로그인", description = "Access Token은 본문으로, Refresh Token은 HttpOnly 쿠키로 내려간다.")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return respondWithTokens(authService.login(request), response);
    }

    @Operation(summary = "Access Token 재발급", description = "refresh_token 쿠키로 호출한다. Refresh Token도 새로 바뀐다.")
    @PostMapping("/reissue")
    public TokenResponse reissue(
            @CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        return respondWithTokens(authService.reissue(refreshToken), response);
    }

    @Operation(summary = "로그아웃", description = "현재 Access Token을 무효화하고 Refresh Token 쿠키를 지운다.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @AuthenticationPrincipal Jwt jwt,
            @CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(jwt.getId(), jwt.getExpiresAt(), refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.clear().toString());
    }

    @Operation(summary = "비밀번호 재설정", description = "이메일과 이름이 일치하면 새 비밀번호로 바꾸고 모든 기기에서 로그아웃시킨다.")
    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
    }

    private TokenResponse respondWithTokens(TokenPair tokens, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.create(tokens.refreshToken()).toString());
        return authMapper.toTokenResponse(tokens);
    }
}
