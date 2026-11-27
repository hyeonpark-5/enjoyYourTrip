package com.hyeon.enjoyyourtrip.domain.plan.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hyeon.enjoyyourtrip.domain.plan.dto.Request.PlanCreateRequest;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Request.PlanUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Response.PlanDetailResponse;
import com.hyeon.enjoyyourtrip.domain.plan.dto.Response.PlanSummaryResponse;
import com.hyeon.enjoyyourtrip.domain.plan.service.PlanService;
import com.hyeon.enjoyyourtrip.global.security.CurrentMemberId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Plan", description = "나만의 여행 계획 — 본인 것만")
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @Operation(summary = "내 여행 계획 목록", description = "최신 작성순. 동선 요약과 총 직선거리 포함")
    @GetMapping
    public List<PlanSummaryResponse> findMine(@CurrentMemberId Long memberId) {
        return planService.findMine(memberId);
    }

    @Operation(summary = "여행 계획 상세")
    @GetMapping("/{id}")
    public PlanDetailResponse get(@PathVariable Long id, @CurrentMemberId Long memberId) {
        return planService.getMine(id, memberId);
    }

    @Operation(summary = "여행 계획 만들기", description = "stops는 방문 순서대로 관광지 검색 결과의 id·name·address·lat·lng·contentTypeId")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanDetailResponse create(@Valid @RequestBody PlanCreateRequest request, @CurrentMemberId Long memberId) {
        return planService.create(request, memberId);
    }

    @Operation(summary = "여행 계획 수정 (전체 교체)")
    @PutMapping("/{id}")
    public PlanDetailResponse update(
            @PathVariable Long id, @Valid @RequestBody PlanUpdateRequest request, @CurrentMemberId Long memberId) {
        return planService.update(id, request, memberId);
    }

    @Operation(summary = "여행 계획 삭제")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentMemberId Long memberId) {
        planService.delete(id, memberId);
    }
}
