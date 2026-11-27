package com.hyeon.enjoyyourtrip.domain.tour.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.hyeon.enjoyyourtrip.domain.tour.dto.Request.NearbySearchRequest;
import com.hyeon.enjoyyourtrip.domain.tour.dto.Request.PlaceSearchRequest;
import com.hyeon.enjoyyourtrip.domain.tour.dto.Response.PlaceResponse;
import com.hyeon.enjoyyourtrip.domain.tour.dto.Response.RegionResponse;
import com.hyeon.enjoyyourtrip.domain.tour.entity.Region;
import com.hyeon.enjoyyourtrip.domain.tour.exception.PlaceNotFoundException;
import com.hyeon.enjoyyourtrip.domain.tour.mapper.TourMapper;
import com.hyeon.enjoyyourtrip.infrastructure.tour.dto.PlaceInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    /** TourAPI 위치 기반 조회의 최대 반경(m) */
    static final int MAX_RADIUS = 20_000;
    static final int FEATURED_LIMIT = 12;
    static final String TOURIST_SPOT = "12";

    private final TourPlaceClient tourPlaceClient;
    private final TourMapper tourMapper;

    @Override
    public List<RegionResponse> regions() {
        return Arrays.stream(Region.values()).map(tourMapper::toResponse).toList();
    }

    @Override
    public List<PlaceResponse> search(PlaceSearchRequest request) {
        String areaCode = request.areaCodeOrNull();
        String contentTypeId = request.contentTypeIdOrNull();
        List<PlaceInfo> places;
        if (request.hasKeyword()) {
            places = tourPlaceClient.searchByKeyword(request.keyword().trim(), areaCode, contentTypeId);
        } else if (areaCode == null) {
            places = List.of();
        } else {
            Optional<Region> region = Region.fromCode(areaCode);
            places = region.isPresent()
                    ? tourPlaceClient.findNearby(region.get().getLat(), region.get().getLng(), MAX_RADIUS, contentTypeId)
                    : tourPlaceClient.findByArea(areaCode, contentTypeId);
        }
        return toResponses(places);
    }

    @Override
    public List<PlaceResponse> findNearby(NearbySearchRequest request) {
        int radius = Math.min(request.radius(), MAX_RADIUS);
        return toResponses(tourPlaceClient.findNearby(
                request.lat(), request.lng(), radius, request.contentTypeIdOrNull()));
    }

    @Override
    public PlaceResponse getDetail(String contentId) {
        return tourPlaceClient.findDetail(contentId)
                .map(tourMapper::toResponse)
                .orElseThrow(() -> new PlaceNotFoundException(contentId));
    }

    @Override
    public List<PlaceResponse> featured() {
        List<PlaceInfo> withImage = new ArrayList<>(tourPlaceClient.findByArea(null, TOURIST_SPOT).stream()
                .filter(place -> !place.image().isBlank())
                .toList());
        Collections.shuffle(withImage);
        return toResponses(withImage.stream().limit(FEATURED_LIMIT).toList());
    }

    private List<PlaceResponse> toResponses(List<PlaceInfo> places) {
        return places.stream().map(tourMapper::toResponse).toList();
    }
}
