package com.hyeon.enjoyyourtrip.domain.hotplace.exception;

import com.hyeon.enjoyyourtrip.global.exception.NotFoundException;

public class HotplaceNotFoundException extends NotFoundException {

    public HotplaceNotFoundException(Long id) {
        super("HOTPLACE_NOT_FOUND", "핫플레이스를 찾을 수 없습니다. id=" + id);
    }
}
