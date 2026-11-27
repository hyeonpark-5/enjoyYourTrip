package com.hyeon.enjoyyourtrip.global.exception;

/** 형식은 맞지만 비즈니스 규칙상 허용되지 않는 값 → 400 */
public abstract class InvalidValueException extends BusinessException {

    protected InvalidValueException(String code, String message) {
        super(code, message);
    }
}
