package com.hyeon.enjoyyourtrip.domain.tour.dto.Response;

/**
 * 관광지. 값이 없는 문자열 필드는 빈 문자열이다.
 *
 * @param id       TourAPI contentId (상세 조회·핫플레이스·여행계획에서 장소 식별자로 쓴다)
 * @param image    대표 이미지 URL
 * @param overview 개요 — 상세 조회에서만 채워진다
 */
public record PlaceResponse(
        String id,
        String name,
        String areaCode,
        String contentTypeId,
        String address,
        String image,
        String tel,
        String overview,
        double lat,
        double lng) {
}
