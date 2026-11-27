package com.hyeon.enjoyyourtrip.domain.plan.dto.Response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 계획 목록 한 줄.
 *
 * @param budget       원 단위, 0이면 미정
 * @param distanceKm   방문 순서대로 이은 총 직선거리 (소수 첫째 자리)
 * @param routeSummary 앞의 4곳 이름 (예: "경복궁 → 남산타워 → … 외 2곳")
 */
public record PlanSummaryResponse(
        Long id,
        String name,
        long budget,
        LocalDate startDate,
        LocalDate endDate,
        int stopCount,
        double distanceKm,
        String routeSummary,
        LocalDateTime createdAt) {
}
