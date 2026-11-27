package com.hyeon.enjoyyourtrip.domain.plan.exception;

import com.hyeon.enjoyyourtrip.global.exception.InvalidValueException;

/** 같은 장소를 동선에 두 번 넣음 */
public class DuplicatePlanStopException extends InvalidValueException {

    public DuplicatePlanStopException(String placeName) {
        super("DUPLICATE_PLAN_STOP", placeName + "은(는) 이미 여행 동선에 추가되어 있습니다.");
    }
}
