package com.hyeon.enjoyyourtrip.global.exception;

/** 대상이 없음 → 404 */
public abstract class NotFoundException extends BusinessException {

    protected NotFoundException(String code, String message) {
        super(code, message);
    }
}
