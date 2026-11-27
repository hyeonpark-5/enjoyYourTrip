package com.hyeon.enjoyyourtrip.domain.tour.dto.Request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 지도 중심 좌표 주변 검색 (쿼리 파라미터).
 *
 * @param radius 반경(m). TourAPI 최대값인 20000을 넘으면 20000으로 맞춘다.
 */
public record NearbySearchRequest(
        @NotNull(message = "위도(lat)는 필수입니다.")
        @DecimalMin(value = "-90", message = "위도가 올바르지 않습니다.")
        @DecimalMax(value = "90", message = "위도가 올바르지 않습니다.")
        Double lat,

        @NotNull(message = "경도(lng)는 필수입니다.")
        @DecimalMin(value = "-180", message = "경도가 올바르지 않습니다.")
        @DecimalMax(value = "180", message = "경도가 올바르지 않습니다.")
        Double lng,

        @NotNull(message = "반경(radius)은 필수입니다.")
        @Min(value = 1, message = "반경은 1m 이상이어야 합니다.")
        Integer radius,

        @Pattern(regexp = "\\d{2}", message = "콘텐츠 타입이 올바르지 않습니다.")
        String contentTypeId) {

    public String contentTypeIdOrNull() {
        return contentTypeId == null || contentTypeId.isBlank() ? null : contentTypeId;
    }
}
