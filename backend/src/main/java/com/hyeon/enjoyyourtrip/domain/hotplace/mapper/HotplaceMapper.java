package com.hyeon.enjoyyourtrip.domain.hotplace.mapper;

import org.springframework.stereotype.Component;

import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Request.HotplaceCreateRequest;
import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Response.HotplaceResponse;
import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Response.PlaceTypeResponse;
import com.hyeon.enjoyyourtrip.domain.hotplace.entity.Hotplace;
import com.hyeon.enjoyyourtrip.domain.hotplace.entity.PlaceType;
import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.domain.tour.dto.Response.PlaceResponse;

@Component
public class HotplaceMapper {

    private static final String PHOTO_URL = "/api/v1/hotplaces/%d/photo";

    /** 장소 정보는 TourAPI 상세(place)를 복사하고, 유형이 없으면 관광지 종류로 정한다. */
    public Hotplace toEntity(HotplaceCreateRequest request, PlaceResponse place, Member owner, String photoFile) {
        PlaceType placeType = request.placeType() != null
                ? request.placeType()
                : PlaceType.fromContentTypeId(place.contentTypeId());
        return Hotplace.builder()
                .owner(owner)
                .placeId(place.id())
                .placeName(place.name())
                .address(place.address())
                .lat(place.lat())
                .lng(place.lng())
                .contentTypeId(place.contentTypeId())
                .placeImage(place.image())
                .placeType(placeType)
                .visitedDate(request.visitedDate())
                .description(request.description().trim())
                .photoFile(photoFile)
                .build();
    }

    public HotplaceResponse toResponse(Hotplace hotplace) {
        return new HotplaceResponse(
                hotplace.getId(),
                hotplace.getPlaceId(),
                hotplace.getPlaceName(),
                hotplace.getAddress(),
                hotplace.getLat(),
                hotplace.getLng(),
                hotplace.getContentTypeId(),
                hotplace.getPlaceImage(),
                hotplace.getPlaceType(),
                hotplace.getPlaceType().getLabel(),
                hotplace.getVisitedDate(),
                hotplace.getDescription(),
                hotplace.hasPhoto() ? PHOTO_URL.formatted(hotplace.getId()) : null,
                hotplace.getCreatedAt(),
                hotplace.getUpdatedAt());
    }

    public PlaceTypeResponse toResponse(PlaceType type) {
        return new PlaceTypeResponse(type, type.getLabel(), type.getContentTypeId());
    }
}
