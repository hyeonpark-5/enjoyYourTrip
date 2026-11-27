package com.hyeon.enjoyyourtrip.domain.plan.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.domain.member.exception.MemberNotFoundException;
import com.hyeon.enjoyyourtrip.domain.member.repository.MemberRepository;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Request.PlanCreateRequest;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Request.PlanStopRequest;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Request.PlanUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Response.PlanDetailResponse;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Response.PlanSummaryResponse;
import com.hyeon.enjoyyourtrip.domain.plan.entity.TripPlan;
import com.hyeon.enjoyyourtrip.domain.plan.exception.DuplicatePlanStopException;
import com.hyeon.enjoyyourtrip.domain.plan.exception.InvalidPlanPeriodException;
import com.hyeon.enjoyyourtrip.domain.plan.exception.PlanForbiddenException;
import com.hyeon.enjoyyourtrip.domain.plan.exception.PlanNotFoundException;
import com.hyeon.enjoyyourtrip.domain.plan.mapper.PlanMapper;
import com.hyeon.enjoyyourtrip.domain.plan.repository.TripPlanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanServiceImpl implements PlanService {

    private final TripPlanRepository tripPlanRepository;
    private final MemberRepository memberRepository;
    private final PlanMapper planMapper;

    @Override
    public List<PlanSummaryResponse> findMine(Long ownerId) {
        return tripPlanRepository.findByOwnerIdOrderByIdDesc(ownerId).stream()
                .map(planMapper::toSummary)
                .toList();
    }

    @Override
    public PlanDetailResponse getMine(Long id, Long ownerId) {
        return planMapper.toDetail(findMine(id, ownerId));
    }

    @Override
    @Transactional
    public PlanDetailResponse create(PlanCreateRequest request, Long ownerId) {
        validate(request.startDate(), request.endDate(), request.stops());
        Member owner = memberRepository.findByIdAndDeletedAtIsNull(ownerId)
                .orElseThrow(() -> new MemberNotFoundException(ownerId));
        TripPlan plan = TripPlan.builder()
                .owner(owner)
                .name(request.name().trim())
                .budget(budgetOrZero(request.budget()))
                .startDate(request.startDate())
                .endDate(request.endDate())
                .notes(notesOrEmpty(request.notes()))
                .stops(planMapper.toStops(request.stops()))
                .build();
        return planMapper.toDetail(tripPlanRepository.save(plan));
    }

    @Override
    @Transactional
    public PlanDetailResponse update(Long id, PlanUpdateRequest request, Long ownerId) {
        TripPlan plan = findMine(id, ownerId);
        validate(request.startDate(), request.endDate(), request.stops());
        plan.update(
                request.name().trim(),
                budgetOrZero(request.budget()),
                request.startDate(),
                request.endDate(),
                notesOrEmpty(request.notes()),
                planMapper.toStops(request.stops()));
        tripPlanRepository.flush();
        return planMapper.toDetail(plan);
    }

    @Override
    @Transactional
    public void delete(Long id, Long ownerId) {
        tripPlanRepository.delete(findMine(id, ownerId));
    }

    private TripPlan findMine(Long id, Long ownerId) {
        TripPlan plan = tripPlanRepository.findWithStopsById(id).orElseThrow(() -> new PlanNotFoundException(id));
        if (!plan.isOwnedBy(ownerId)) {
            throw new PlanForbiddenException();
        }
        return plan;
    }

    private static void validate(LocalDate startDate, LocalDate endDate, List<PlanStopRequest> stops) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidPlanPeriodException();
        }
        Set<String> seen = new HashSet<>();
        for (PlanStopRequest stop : stops) {
            if (!seen.add(stop.placeId().trim())) {
                throw new DuplicatePlanStopException(stop.name().trim());
            }
        }
    }

    private static long budgetOrZero(Long budget) {
        return budget == null ? 0 : budget;
    }

    private static String notesOrEmpty(String notes) {
        return notes == null ? "" : notes.trim();
    }
}
