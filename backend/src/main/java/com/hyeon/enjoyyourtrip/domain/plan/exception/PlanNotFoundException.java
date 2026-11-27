package com.hyeon.enjoyyourtrip.domain.plan.exception;

import com.hyeon.enjoyyourtrip.global.exception.NotFoundException;

public class PlanNotFoundException extends NotFoundException {

    public PlanNotFoundException(Long id) {
        super("PLAN_NOT_FOUND", "여행 계획을 찾을 수 없습니다. id=" + id);
    }
}
