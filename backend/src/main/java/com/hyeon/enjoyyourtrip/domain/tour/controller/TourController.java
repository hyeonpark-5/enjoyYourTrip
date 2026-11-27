package com.hyeon.enjoyyourtrip.domain.tour.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyeon.enjoyyourtrip.domain.tour.dto.Request.NearbySearchRequest;
import com.hyeon.enjoyyourtrip.domain.tour.dto.Request.PlaceSearchRequest;
import com.hyeon.enjoyyourtrip.domain.tour.dto.Response.PlaceResponse;
import com.hyeon.enjoyyourtrip.domain.tour.dto.Response.RegionResponse;
import com.hyeon.enjoyyourtrip.domain.tour.service.TourService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

@Tag(name = "Tour", description = "관광지 찾기 (한국관광공사 TourAPI) — 로그인 없이 사용")
@RestController
@RequestMapping("/api/v1/tours")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

    @Operation(summary = "지역 목록", description = "17개 시·도와 대표 좌표")
    @GetMapping("/regions")
    public List<RegionResponse> regions() {
        return tourService.regions();
    }

    @Operation(summary = "관광지 검색", description = "areaCode(지역) 또는 keyword 중 하나는 있어야 결과가 나온다. contentTypeId로 종류를 거른다.")
    @GetMapping("/places")
    public List<PlaceResponse> search(@Valid @ModelAttribute PlaceSearchRequest request) {
        return tourService.search(request);
    }

    @Operation(summary = "주변 관광지", description = "지도 중심(lat, lng) 반경 radius(m, 최대 20000) 안을 거리순으로")
    @GetMapping("/places/nearby")
    public List<PlaceResponse> nearby(@Valid @ModelAttribute NearbySearchRequest request) {
        return tourService.findNearby(request);
    }

    @Operation(summary = "추천 관광지", description = "사진이 있는 전국 관광지 무작위 최대 12곳")
    @GetMapping("/places/featured")
    public List<PlaceResponse> featured() {
        return tourService.featured();
    }

    @Operation(summary = "관광지 상세", description = "개요(overview) 포함")
    @GetMapping("/places/{contentId}")
    public PlaceResponse detail(
            @PathVariable @Pattern(regexp = "\\d{1,10}", message = "관광지 id가 올바르지 않습니다.") String contentId) {
        return tourService.getDetail(contentId);
    }
}
