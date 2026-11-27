package com.hyeon.enjoyyourtrip.infrastructure.tour.dto;

import java.io.Serializable;

/**
 * 내 도메인용 관광지 정보. TourAPI 응답 스키마(TourApiItem)가 바뀌어도 도메인 코드는 이 타입만 본다.
 * Redis 캐시에 저장되므로 Serializable이다.
 *
 * @param id            TourAPI contentId
 * @param contentTypeId 12 관광지, 14 문화시설, 15 축제, 25 여행코스, 28 레포츠, 32 숙박, 38 쇼핑, 39 음식점
 * @param overview      개요(HTML 태그 제거). 목록 조회에서는 빈 문자열
 */
public record PlaceInfo(
        String id,
        String name,
        String areaCode,
        String contentTypeId,
        String address,
        String image,
        String tel,
        String overview,
        double lat,
        double lng) implements Serializable {
}
