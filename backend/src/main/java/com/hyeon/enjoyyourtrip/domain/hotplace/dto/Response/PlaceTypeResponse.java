package com.hyeon.enjoyyourtrip.domain.hotplace.dto.Response;

import com.hyeon.enjoyyourtrip.domain.hotplace.entity.PlaceType;

/** 장소 유형 선택지 */
public record PlaceTypeResponse(PlaceType code, String label, String contentTypeId) {
}
