package com.hyeon.enjoyyourtrip.domain.tour.service;

import java.util.List;
import java.util.Optional;

import com.hyeon.enjoyyourtrip.infrastructure.tour.dto.PlaceInfo;

/**
 * 외부 관광정보 조회 도메인 인터페이스(DIP). 구현체는 infrastructure.tour.service.TourPlaceClientImpl.
 * 외부 호출 실패는 TourApiUnavailableException(502)으로 던진다.
 * 좌표가 없거나 국내 범위를 벗어난 항목은 결과에서 제외된다.
 */
public interface TourPlaceClient {

    /** 지역·콘텐츠 타입별 목록 (null이면 조건 없음) */
    List<PlaceInfo> findByArea(String areaCode, String contentTypeId);

    List<PlaceInfo> searchByKeyword(String keyword, String areaCode, String contentTypeId);

    /** 중심 좌표 반경(m) 안의 관광지를 거리순으로 */
    List<PlaceInfo> findNearby(double lat, double lng, int radius, String contentTypeId);

    /** 개요(overview)를 포함한 상세. 없으면 empty */
    Optional<PlaceInfo> findDetail(String contentId);
}
