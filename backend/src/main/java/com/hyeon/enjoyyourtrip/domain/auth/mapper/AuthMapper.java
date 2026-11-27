package com.hyeon.enjoyyourtrip.domain.auth.mapper;

import org.springframework.stereotype.Component;

import com.hyeon.enjoyyourtrip.domain.auth.dto.Request.SignupRequest;
import com.hyeon.enjoyyourtrip.domain.auth.dto.Response.TokenResponse;
import com.hyeon.enjoyyourtrip.domain.auth.service.TokenPair;
import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.domain.member.entity.Role;

@Component
public class AuthMapper {

    private static final String BEARER = "Bearer";

    /** 가입 회원은 항상 USER 권한으로 만든다. */
    public Member toEntity(SignupRequest request, String normalizedEmail, String encodedPassword) {
        return Member.create(normalizedEmail, encodedPassword, request.name().trim(), Role.USER);
    }

    public TokenResponse toTokenResponse(TokenPair tokens) {
        return new TokenResponse(tokens.accessToken(), BEARER, tokens.accessTokenExpiresIn());
    }
}
