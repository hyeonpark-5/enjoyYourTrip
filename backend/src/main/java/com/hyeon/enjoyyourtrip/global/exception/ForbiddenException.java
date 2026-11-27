package com.hyeon.enjoyyourtrip.global.exception;

/** 로그인은 했지만 권한 없음(남의 글 수정 등) → 403 */
public abstract class ForbiddenException extends BusinessException {

    protected ForbiddenException(String code, String message) {
        super(code, message);
    }
}
