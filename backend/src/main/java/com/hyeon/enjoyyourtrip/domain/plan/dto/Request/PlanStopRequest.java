package com.hyeon.enjoyyourtrip.domain.plan.dto.Request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 동선에 넣을 장소 하나. 관광지 검색 결과(PlaceResponse)의 값을 그대로 보내면 된다.
 * 좌표는 국내 범위여야 한다.
 */
public record PlanStopRequest(
        @NotBlank(message = "장소 id는 필수입니다.")
        @Pattern(regexp = "[A-Za-z0-9_-]{1,40}", message = "장소 id가 올바르지 않습니다.")
        String placeId,

        @NotBlank(message = "장소 이름은 필수입니다.")
        @Size(max = 100, message = "장소 이름은 100자 이내여야 합니다.")
        String name,

        @Size(max = 200, message = "주소는 200자 이내여야 합니다.")
        String address,

        @NotNull(message = "위도는 필수입니다.")
        @DecimalMin(value = "32.5", message = "국내 좌표가 아닙니다.")
        @DecimalMax(value = "39.0", message = "국내 좌표가 아닙니다.")
        Double lat,

        @NotNull(message = "경도는 필수입니다.")
        @DecimalMin(value = "123.5", message = "국내 좌표가 아닙니다.")
        @DecimalMax(value = "132.0", message = "국내 좌표가 아닙니다.")
        Double lng,

        @Pattern(regexp = "\\d{0,5}", message = "콘텐츠 타입이 올바르지 않습니다.")
        String contentTypeId) {
}
