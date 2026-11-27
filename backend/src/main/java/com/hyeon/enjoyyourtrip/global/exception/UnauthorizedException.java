package com.hyeon.enjoyyourtrip.global.exception;

/** 인증 실패(로그인 실패, 토큰 무효 등) → 401 */
public abstract class UnauthorizedException extends BusinessException {

    protected UnauthorizedException(String code, String message) {
        super(code, message);
    }
}
