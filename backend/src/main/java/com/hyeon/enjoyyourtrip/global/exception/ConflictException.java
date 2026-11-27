package com.hyeon.enjoyyourtrip.global.exception;

/** 중복 등 현재 상태와 충돌 → 409 */
public abstract class ConflictException extends BusinessException {

    protected ConflictException(String code, String message) {
        super(code, message);
    }
}
