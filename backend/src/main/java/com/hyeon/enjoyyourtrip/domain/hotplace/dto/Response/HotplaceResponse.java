package com.hyeon.enjoyyourtrip.domain.hotplace.dto.Response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hyeon.enjoyyourtrip.domain.hotplace.entity.PlaceType;

/**
 * @param placeImage TourAPI 대표 이미지 URL (빈 문자열이면 없음)
 * @param photoUrl   직접 올린 사진 주소(없으면 null). 본인만 볼 수 있어 Authorization 헤더가 필요하므로
 *                   프론트엔드는 fetch로 받아 Blob URL로 표시한다.
 */
public record HotplaceResponse(
        Long id,
        String placeId,
        String placeName,
        String address,
        double lat,
        double lng,
        String contentTypeId,
        String placeImage,
        PlaceType placeType,
        String placeTypeLabel,
        LocalDate visitedDate,
        String description,
        String photoUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
