package com.hyeon.enjoyyourtrip.domain.hotplace.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Request.HotplaceCreateRequest;
import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Request.HotplaceUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Response.HotplaceResponse;
import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Response.PlaceTypeResponse;
import com.hyeon.enjoyyourtrip.domain.hotplace.service.HotplaceService;
import com.hyeon.enjoyyourtrip.global.security.CurrentMemberId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Hotplace", description = "핫플레이스 (내가 다녀온 장소 기록) — 본인 것만")
@RestController
@RequestMapping("/api/v1/hotplaces")
@RequiredArgsConstructor
public class HotplaceController {

    private final HotplaceService hotplaceService;

    @Operation(summary = "장소 유형 목록")
    @GetMapping("/place-types")
    public List<PlaceTypeResponse> placeTypes() {
        return hotplaceService.placeTypes();
    }

    @Operation(summary = "내 핫플레이스 목록", description = "최신 등록순")
    @GetMapping
    public List<HotplaceResponse> findMine(@CurrentMemberId Long memberId) {
        return hotplaceService.findMine(memberId);
    }

    @Operation(summary = "핫플레이스 상세")
    @GetMapping("/{id}")
    public HotplaceResponse get(@PathVariable Long id, @CurrentMemberId Long memberId) {
        return hotplaceService.getMine(id, memberId);
    }

    @Operation(summary = "핫플레이스 등록", description = "multipart/form-data: placeId, placeType(선택), visitedDate, description, photo(선택, 8MB 이하)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public HotplaceResponse create(
            @Valid @ModelAttribute HotplaceCreateRequest request,
            @RequestPart(name = "photo", required = false) MultipartFile photo,
            @CurrentMemberId Long memberId) {
        return hotplaceService.create(request, bytes(photo), memberId);
    }

    @Operation(summary = "핫플레이스 수정", description = "multipart/form-data: placeType, visitedDate, description, removePhoto, photo(선택)")
    @PatchMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public HotplaceResponse update(
            @PathVariable Long id,
            @Valid @ModelAttribute HotplaceUpdateRequest request,
            @RequestPart(name = "photo", required = false) MultipartFile photo,
            @CurrentMemberId Long memberId) {
        return hotplaceService.update(id, request, bytes(photo), memberId);
    }

    @Operation(summary = "핫플레이스 삭제")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentMemberId Long memberId) {
        hotplaceService.delete(id, memberId);
    }

    @Operation(summary = "핫플레이스 사진", description = "image/jpeg. 본인만 볼 수 있다.")
    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> photo(@PathVariable Long id, @CurrentMemberId Long memberId) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(hotplaceService.getPhoto(id, memberId));
    }

    private static byte[] bytes(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            return null;
        }
        try {
            return photo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("업로드한 사진을 읽지 못했습니다.", e);
        }
    }
}
