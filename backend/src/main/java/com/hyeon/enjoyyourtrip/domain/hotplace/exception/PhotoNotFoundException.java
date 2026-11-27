package com.hyeon.enjoyyourtrip.domain.hotplace.exception;

import com.hyeon.enjoyyourtrip.global.exception.NotFoundException;

public class PhotoNotFoundException extends NotFoundException {

    public PhotoNotFoundException(Long hotplaceId) {
        super("PHOTO_NOT_FOUND", "등록된 사진이 없습니다. hotplaceId=" + hotplaceId);
    }
}
