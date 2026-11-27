package com.hyeon.enjoyyourtrip.domain.plan.mapper;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.hyeon.enjoyyourtrip.domain.plan.dto.Request.PlanStopRequest;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Response.PlanDetailResponse;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Response.PlanStopResponse;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Response.PlanSummaryResponse;
import com.hyeon.enjoyyourtrip.domain.plan.entity.PlanStop;
import com.hyeon.enjoyyourtrip.domain.plan.entity.TripPlan;

@Component
public class PlanMapper {

    private static final int SUMMARY_STOPS = 4;

    public List<PlanStop> toStops(List<PlanStopRequest> requests) {
        return requests.stream()
                .map(stop -> new PlanStop(
                        stop.placeId().trim(),
                        stop.name().trim(),
                        stop.address() == null ? "" : stop.address().trim(),
                        stop.lat(),
                        stop.lng(),
                        stop.contentTypeId() == null ? "" : stop.contentTypeId()))
                .toList();
    }

    public PlanSummaryResponse toSummary(TripPlan plan) {
        return new PlanSummaryResponse(
                plan.getId(),
                plan.getName(),
                plan.getBudget(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getStops().size(),
                round(plan.distanceKm()),
                routeSummary(plan.getStops()),
                plan.getCreatedAt());
    }

    public PlanDetailResponse toDetail(TripPlan plan) {
        List<PlanStop> stops = plan.getStops();
        List<PlanStopResponse> stopResponses = IntStream.range(0, stops.size())
                .mapToObj(i -> toResponse(i + 1, stops.get(i)))
                .toList();
        return new PlanDetailResponse(
                plan.getId(),
                plan.getName(),
                plan.getBudget(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getNotes(),
                stopResponses,
                round(plan.distanceKm()),
                plan.getCreatedAt(),
                plan.getUpdatedAt());
    }

    private static PlanStopResponse toResponse(int order, PlanStop stop) {
        return new PlanStopResponse(order, stop.getPlaceId(), stop.getName(), stop.getAddress(),
                stop.getLat(), stop.getLng(), stop.getContentTypeId());
    }

    /** 예: 경복궁 → 남산타워 → 북촌 → 인사동 → … 외 2곳 */
    private static String routeSummary(List<PlanStop> stops) {
        String joined = stops.stream().limit(SUMMARY_STOPS).map(PlanStop::getName).collect(Collectors.joining(" → "));
        return stops.size() > SUMMARY_STOPS ? joined + " → … 외 " + (stops.size() - SUMMARY_STOPS) + "곳" : joined;
    }

    private static double round(double km) {
        return Math.round(km * 10) / 10.0;
    }
}
