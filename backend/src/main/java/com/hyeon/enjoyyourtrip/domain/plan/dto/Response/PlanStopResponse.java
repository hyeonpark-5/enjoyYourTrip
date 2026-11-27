package com.hyeon.enjoyyourtrip.domain.plan.dto.Response;

/** 동선의 한 장소. order는 1부터 시작하는 방문 순서 */
public record PlanStopResponse(
        int order,
        String placeId,
        String name,
        String address,
        double lat,
        double lng,
        String contentTypeId) {
}
