package com.hyeon.enjoyyourtrip.domain.plan.exception;

import com.hyeon.enjoyyourtrip.global.exception.ForbiddenException;

public class PlanForbiddenException extends ForbiddenException {

    public PlanForbiddenException() {
        super("PLAN_FORBIDDEN", "본인의 여행 계획만 보거나 바꿀 수 있습니다.");
    }
}
