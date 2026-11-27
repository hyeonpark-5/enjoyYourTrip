package com.hyeon.enjoyyourtrip.domain.tour.dto.Request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 관광지 검색 (쿼리 파라미터). 지역도 키워드도 없으면 빈 목록을 돌려준다(전국 전체 조회 방지).
 *
 * @param areaCode      지역 코드 (GET /api/v1/tours/regions), "all"이면 전국
 * @param contentTypeId 콘텐츠 타입 (12 관광지, 14 문화시설, 15 축제, 25 여행코스, 28 레포츠, 32 숙박, 38 쇼핑, 39 음식점)
 * @param keyword       관광지명 검색어
 */
public record PlaceSearchRequest(
        @Pattern(regexp = "all|\\d{1,2}", message = "지역 코드가 올바르지 않습니다.")
        String areaCode,

        @Pattern(regexp = "\\d{2}", message = "콘텐츠 타입이 올바르지 않습니다.")
        String contentTypeId,

        @Size(max = 30, message = "검색어는 30자 이내로 입력해주세요.")
        String keyword) {

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }

    /** "all"이거나 비어 있으면 null */
    public String areaCodeOrNull() {
        return areaCode == null || areaCode.isBlank() || "all".equals(areaCode) ? null : areaCode;
    }

    public String contentTypeIdOrNull() {
        return contentTypeId == null || contentTypeId.isBlank() ? null : contentTypeId;
    }
}
