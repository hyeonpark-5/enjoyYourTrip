package com.hyeon.enjoyyourtrip.domain.tour.mapper;

import org.springframework.stereotype.Component;

import com.hyeon.enjoyyourtrip.domain.tour.dto.Response.PlaceResponse;
import com.hyeon.enjoyyourtrip.domain.tour.dto.Response.RegionResponse;
import com.hyeon.enjoyyourtrip.domain.tour.entity.Region;
import com.hyeon.enjoyyourtrip.infrastructure.tour.dto.PlaceInfo;

@Component
public class TourMapper {

    public PlaceResponse toResponse(PlaceInfo place) {
        return new PlaceResponse(
                place.id(),
                place.name(),
                place.areaCode(),
                place.contentTypeId(),
                place.address(),
                place.image(),
                place.tel(),
                place.overview(),
                place.lat(),
                place.lng());
    }

    public RegionResponse toResponse(Region region) {
        return new RegionResponse(region.getCode(), region.getKoreanName(), region.getLat(), region.getLng());
    }
}
