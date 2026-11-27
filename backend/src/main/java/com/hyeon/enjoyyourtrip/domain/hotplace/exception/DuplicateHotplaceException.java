package com.hyeon.enjoyyourtrip.domain.hotplace.exception;

import com.hyeon.enjoyyourtrip.global.exception.ConflictException;

/** 같은 장소는 하루에 한 번만 등록할 수 있다. */
public class DuplicateHotplaceException extends ConflictException {

    public DuplicateHotplaceException(String placeName) {
        super("DUPLICATE_HOTPLACE",
                placeName + "은(는) 선택한 날짜에 이미 등록되어 있습니다. 같은 장소는 하루에 한 번만 등록할 수 있습니다.");
    }
}
