package com.hyeon.enjoyyourtrip.domain.plan.service;

import java.util.List;

import com.hyeon.enjoyyourtrip.domain.plan.dto.Request.PlanCreateRequest;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Request.PlanUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Response.PlanDetailResponse;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Response.PlanSummaryResponse;

/**
 * 나만의 여행 계획. 모든 기능은 본인 계획에만 쓸 수 있다.
 */
public interface PlanService {

    /** 최신 작성순 */
    List<PlanSummaryResponse> findMine(Long ownerId);

    PlanDetailResponse getMine(Long id, Long ownerId);

    /** 도착일이 출발일보다 빠르거나 동선에 같은 장소가 두 번 있으면 400 */
    PlanDetailResponse create(PlanCreateRequest request, Long ownerId);

    /** 이름·경비·기간·메모·동선을 모두 바꾼다. */
    PlanDetailResponse update(Long id, PlanUpdateRequest request, Long ownerId);

    void delete(Long id, Long ownerId);
}
