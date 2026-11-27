package com.hyeon.enjoyyourtrip.domain.tour.exception;

import com.hyeon.enjoyyourtrip.global.exception.NotFoundException;

public class PlaceNotFoundException extends NotFoundException {

    public PlaceNotFoundException(String contentId) {
        super("PLACE_NOT_FOUND", "관광지 정보를 찾을 수 없습니다. id=" + contentId);
    }
}
