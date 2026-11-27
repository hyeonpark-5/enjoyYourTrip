package com.hyeon.enjoyyourtrip.domain.plan.dto.Response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 계획 상세.
 *
 * @param distanceKm 방문 순서대로 이은 총 직선거리 (소수 첫째 자리)
 */
public record PlanDetailResponse(
        Long id,
        String name,
        long budget,
        LocalDate startDate,
        LocalDate endDate,
        String notes,
        List<PlanStopResponse> stops,
        double distanceKm,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
