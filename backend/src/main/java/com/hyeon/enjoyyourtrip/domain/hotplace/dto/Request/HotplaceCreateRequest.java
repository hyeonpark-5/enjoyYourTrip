package com.hyeon.enjoyyourtrip.domain.hotplace.dto.Request;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.hyeon.enjoyyourtrip.domain.hotplace.entity.PlaceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 핫플레이스 등록 (multipart/form-data 필드). 사진은 별도 파트 photo로 보낸다(선택).
 *
 * @param placeId     관광지 id (GET /api/v1/tours/places의 id)
 * @param placeType   장소 유형 — 비우면 관광지 종류에 맞춰 정한다
 * @param visitedDate 다녀온 날짜 (yyyy-MM-dd, 오늘 이후 불가)
 */
public record HotplaceCreateRequest(
        @NotBlank(message = "관광지를 선택해 주세요.")
        @Pattern(regexp = "\\d{1,10}", message = "관광지 id가 올바르지 않습니다.")
        String placeId,

        PlaceType placeType,

        @NotNull(message = "다녀온 날짜를 선택해 주세요.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate visitedDate,

        @NotBlank(message = "상세 설명을 입력해 주세요.")
        @Size(max = 500, message = "상세 설명은 500자 이내로 입력해 주세요.")
        String description) {
}
