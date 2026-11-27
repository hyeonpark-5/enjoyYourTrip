package com.hyeon.enjoyyourtrip.domain.hotplace.exception;

import com.hyeon.enjoyyourtrip.global.exception.ForbiddenException;

public class HotplaceForbiddenException extends ForbiddenException {

    public HotplaceForbiddenException() {
        super("HOTPLACE_FORBIDDEN", "본인의 핫플레이스만 보거나 바꿀 수 있습니다.");
    }
}
