package com.hyeon.enjoyyourtrip.domain.hotplace.entity;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 핫플레이스 장소 유형. contentTypeId는 대응하는 TourAPI 콘텐츠 타입(기본값을 정할 때 쓴다).
 */
@Getter
@RequiredArgsConstructor
public enum PlaceType {
    TOURIST_SPOT("관광지", "12"),
    CULTURE("문화시설", "14"),
    FESTIVAL("축제·공연·행사", "15"),
    COURSE("여행코스", "25"),
    LEISURE("레포츠", "28"),
    LODGING("숙박", "32"),
    SHOPPING("쇼핑", "38"),
    RESTAURANT("음식점", "39"),
    ETC("기타", null);

    private final String label;
    private final String contentTypeId;

    /** TourAPI 콘텐츠 타입에 맞는 유형. 대응하는 것이 없으면 ETC */
    public static PlaceType fromContentTypeId(String contentTypeId) {
        return Arrays.stream(values())
                .filter(type -> type.contentTypeId != null && type.contentTypeId.equals(contentTypeId))
                .findFirst()
                .orElse(ETC);
    }
}
