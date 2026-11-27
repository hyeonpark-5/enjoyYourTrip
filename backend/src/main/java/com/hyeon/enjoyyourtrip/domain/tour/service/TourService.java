package com.hyeon.enjoyyourtrip.domain.tour.service;

import java.util.List;

import com.hyeon.enjoyyourtrip.domain.tour.dto.Request.NearbySearchRequest;
import com.hyeon.enjoyyourtrip.domain.tour.dto.Request.PlaceSearchRequest;
import com.hyeon.enjoyyourtrip.domain.tour.dto.Response.PlaceResponse;
import com.hyeon.enjoyyourtrip.domain.tour.dto.Response.RegionResponse;

/**
 * 관광지 찾기. 데이터는 TourPlaceClient(외부 TourAPI)에서 온다.
 */
public interface TourService {

    List<RegionResponse> regions();

    /**
     * 키워드가 있으면 키워드 검색, 없고 알려진 지역이면 그 지역 대표 좌표 반경 20km 검색.
     * (TourAPI의 areaCode 필터가 지역을 제대로 거르지 못해 원본에서 쓰던 방식)
     * 지역도 키워드도 없으면 빈 목록.
     */
    List<PlaceResponse> search(PlaceSearchRequest request);

    List<PlaceResponse> findNearby(NearbySearchRequest request);

    PlaceResponse getDetail(String contentId);

    /** 메인 화면 "지금 떠나기 좋은 곳": 사진이 있는 전국 관광지를 무작위로 최대 12곳 */
    List<PlaceResponse> featured();
}
