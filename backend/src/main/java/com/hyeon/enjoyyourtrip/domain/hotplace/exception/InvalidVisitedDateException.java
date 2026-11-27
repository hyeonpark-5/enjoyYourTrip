package com.hyeon.enjoyyourtrip.domain.hotplace.exception;

import com.hyeon.enjoyyourtrip.global.exception.InvalidValueException;

public class InvalidVisitedDateException extends InvalidValueException {

    public InvalidVisitedDateException() {
        super("INVALID_VISITED_DATE", "다녀온 날짜는 오늘 이후로 선택할 수 없습니다.");
    }
}
