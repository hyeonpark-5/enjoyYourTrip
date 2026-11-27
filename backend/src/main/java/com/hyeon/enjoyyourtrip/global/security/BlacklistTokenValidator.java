package com.hyeon.enjoyyourtrip.global.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import lombok.RequiredArgsConstructor;

/** 로그아웃 처리된 토큰(jti가 블랙리스트에 있음)을 거부한다. */
@RequiredArgsConstructor
public class BlacklistTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error LOGGED_OUT =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "로그아웃된 토큰입니다.", null);

    private final AccessTokenBlacklist blacklist;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        return blacklist.contains(token.getId())
                ? OAuth2TokenValidatorResult.failure(LOGGED_OUT)
                : OAuth2TokenValidatorResult.success();
    }
}
