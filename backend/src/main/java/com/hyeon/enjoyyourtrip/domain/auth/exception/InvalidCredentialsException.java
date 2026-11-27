package com.hyeon.enjoyyourtrip.domain.auth.exception;

import com.hyeon.enjoyyourtrip.global.exception.UnauthorizedException;

/** 이메일이 없는지 비밀번호가 틀렸는지 구분하지 않는다(계정 존재 여부 노출 방지). */
public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException() {
        super("LOGIN_FAILED", "이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
