package com.hyeon.enjoyyourtrip.domain.tour.dto.Response;

/** 지역 선택 목록 한 줄 (지도 초기 중심 좌표 포함) */
public record RegionResponse(String code, String name, double lat, double lng) {
}
