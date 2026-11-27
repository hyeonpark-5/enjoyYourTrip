package com.hyeon.enjoyyourtrip.domain.hotplace.dto.Request;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.hyeon.enjoyyourtrip.domain.hotplace.entity.PlaceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 핫플레이스 수정 (multipart/form-data 필드). 장소는 바꿀 수 없다.
 * 사진: 새 photo 파트를 보내면 교체, removePhoto=true면 삭제, 둘 다 없으면 유지.
 */
public record HotplaceUpdateRequest(
        @NotNull(message = "장소 유형을 선택해 주세요.")
        PlaceType placeType,

        @NotNull(message = "다녀온 날짜를 선택해 주세요.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate visitedDate,

        @NotBlank(message = "상세 설명을 입력해 주세요.")
        @Size(max = 500, message = "상세 설명은 500자 이내로 입력해 주세요.")
        String description,

        Boolean removePhoto) {

    /** removePhoto를 보내지 않으면 false */
    public boolean shouldRemovePhoto() {
        return Boolean.TRUE.equals(removePhoto);
    }
}
