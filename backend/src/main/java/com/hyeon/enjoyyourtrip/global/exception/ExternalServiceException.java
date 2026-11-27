package com.hyeon.enjoyyourtrip.global.exception;

/** 외부 API(TourAPI 등) 호출 실패 → 502 */
public abstract class ExternalServiceException extends BusinessException {

    protected ExternalServiceException(String code, String message, Throwable cause) {
        super(code, message);
        initCause(cause);
    }
}
