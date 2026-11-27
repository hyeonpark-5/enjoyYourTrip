package com.hyeon.enjoyyourtrip.domain.auth.exception;

import com.hyeon.enjoyyourtrip.global.exception.UnauthorizedException;

/** Refresh Token이 없거나 만료·폐기됨 → 다시 로그인해야 한다. */
public class InvalidRefreshTokenException extends UnauthorizedException {

    public InvalidRefreshTokenException() {
        super("INVALID_REFRESH_TOKEN", "로그인이 만료되었습니다. 다시 로그인해주세요.");
    }
}
