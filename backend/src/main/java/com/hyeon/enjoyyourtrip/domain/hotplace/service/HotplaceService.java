package com.hyeon.enjoyyourtrip.domain.hotplace.service;

import java.util.List;

import org.springframework.core.io.Resource;

import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Request.HotplaceCreateRequest;
import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Request.HotplaceUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Response.HotplaceResponse;
import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Response.PlaceTypeResponse;

/**
 * 핫플레이스(내가 다녀온 장소 기록). 모든 기능은 본인 것에만 쓸 수 있다.
 * 사진(photo)은 업로드된 원본 바이트이며 없으면 null.
 */
public interface HotplaceService {

    List<PlaceTypeResponse> placeTypes();

    /** 최신 등록순 */
    List<HotplaceResponse> findMine(Long ownerId);

    HotplaceResponse getMine(Long id, Long ownerId);

    /** TourAPI에서 장소 정보를 가져와 저장한다. 같은 장소·같은 날짜는 중복 등록 불가. */
    HotplaceResponse create(HotplaceCreateRequest request, byte[] photo, Long ownerId);

    HotplaceResponse update(Long id, HotplaceUpdateRequest request, byte[] photo, Long ownerId);

    /** 기록과 사진 파일을 함께 지운다. */
    void delete(Long id, Long ownerId);

    Resource getPhoto(Long id, Long ownerId);
}
