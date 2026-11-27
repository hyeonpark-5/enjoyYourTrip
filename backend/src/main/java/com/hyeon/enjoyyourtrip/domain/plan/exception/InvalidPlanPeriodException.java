package com.hyeon.enjoyyourtrip.domain.plan.exception;

import com.hyeon.enjoyyourtrip.global.exception.InvalidValueException;

public class InvalidPlanPeriodException extends InvalidValueException {

    public InvalidPlanPeriodException() {
        super("INVALID_PLAN_PERIOD", "도착일은 출발일보다 빠를 수 없습니다.");
    }
}
