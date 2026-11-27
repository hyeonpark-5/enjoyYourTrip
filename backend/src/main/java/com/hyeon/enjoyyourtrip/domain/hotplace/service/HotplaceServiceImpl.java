package com.hyeon.enjoyyourtrip.domain.hotplace.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Request.HotplaceCreateRequest;
import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Request.HotplaceUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Response.HotplaceResponse;
import com.hyeon.enjoyyourtrip.domain.hotplace.dto.Response.PlaceTypeResponse;
import com.hyeon.enjoyyourtrip.domain.hotplace.entity.Hotplace;
import com.hyeon.enjoyyourtrip.domain.hotplace.entity.PlaceType;
import com.hyeon.enjoyyourtrip.domain.hotplace.exception.DuplicateHotplaceException;
import com.hyeon.enjoyyourtrip.domain.hotplace.exception.HotplaceForbiddenException;
import com.hyeon.enjoyyourtrip.domain.hotplace.exception.HotplaceNotFoundException;
import com.hyeon.enjoyyourtrip.domain.hotplace.exception.InvalidVisitedDateException;
import com.hyeon.enjoyyourtrip.domain.hotplace.exception.PhotoNotFoundException;
import com.hyeon.enjoyyourtrip.domain.hotplace.mapper.HotplaceMapper;
import com.hyeon.enjoyyourtrip.domain.hotplace.repository.HotplaceRepository;
import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.domain.member.exception.MemberNotFoundException;
import com.hyeon.enjoyyourtrip.domain.member.repository.MemberRepository;
import com.hyeon.enjoyyourtrip.domain.tour.dto.Response.PlaceResponse;
import com.hyeon.enjoyyourtrip.domain.tour.service.TourService;

import lombok.RequiredArgsConstructor;

/**
 * 사진 파일과 DB가 어긋나지 않도록 파일 정리는 트랜잭션 결과에 맞춘다.
 * - 새로 저장한 사진: 롤백되면 지운다.
 * - 교체·삭제된 이전 사진: 커밋된 뒤에만 지운다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotplaceServiceImpl implements HotplaceService {

    private final HotplaceRepository hotplaceRepository;
    private final MemberRepository memberRepository;
    private final TourService tourService;
    private final HotplacePhotoStorage photoStorage;
    private final HotplaceMapper hotplaceMapper;
    private final Clock clock;

    @Override
    public List<PlaceTypeResponse> placeTypes() {
        return Arrays.stream(PlaceType.values()).map(hotplaceMapper::toResponse).toList();
    }

    @Override
    public List<HotplaceResponse> findMine(Long ownerId) {
        return hotplaceRepository.findByOwnerIdOrderByIdDesc(ownerId).stream()
                .map(hotplaceMapper::toResponse)
                .toList();
    }

    @Override
    public HotplaceResponse getMine(Long id, Long ownerId) {
        return hotplaceMapper.toResponse(findMine(id, ownerId));
    }

    @Override
    @Transactional
    public HotplaceResponse create(HotplaceCreateRequest request, byte[] photo, Long ownerId) {
        checkVisitedDate(request.visitedDate());
        Member owner = memberRepository.findByIdAndDeletedAtIsNull(ownerId)
                .orElseThrow(() -> new MemberNotFoundException(ownerId));
        PlaceResponse place = tourService.getDetail(request.placeId().trim());
        if (hotplaceRepository.existsByOwnerIdAndPlaceIdAndVisitedDate(ownerId, place.id(), request.visitedDate())) {
            throw new DuplicateHotplaceException(place.name());
        }
        String photoFile = savePhoto(photo);
        Hotplace hotplace = hotplaceRepository.save(hotplaceMapper.toEntity(request, place, owner, photoFile));
        return hotplaceMapper.toResponse(hotplace);
    }

    @Override
    @Transactional
    public HotplaceResponse update(Long id, HotplaceUpdateRequest request, byte[] photo, Long ownerId) {
        Hotplace hotplace = findMine(id, ownerId);
        checkVisitedDate(request.visitedDate());
        if (hotplaceRepository.existsByOwnerIdAndPlaceIdAndVisitedDateAndIdNot(
                ownerId, hotplace.getPlaceId(), request.visitedDate(), id)) {
            throw new DuplicateHotplaceException(hotplace.getPlaceName());
        }
        hotplace.edit(request.placeType(), request.visitedDate(), request.description().trim());

        String oldPhoto = hotplace.getPhotoFile();
        String newPhoto = savePhoto(photo);
        if (newPhoto != null) {
            hotplace.changePhoto(newPhoto);
        } else if (request.shouldRemovePhoto()) {
            hotplace.changePhoto(null);
        }
        if (oldPhoto != null && !oldPhoto.equals(hotplace.getPhotoFile())) {
            deletePhotoAfterCommit(oldPhoto);
        }
        hotplaceRepository.flush();
        return hotplaceMapper.toResponse(hotplace);
    }

    @Override
    @Transactional
    public void delete(Long id, Long ownerId) {
        Hotplace hotplace = findMine(id, ownerId);
        hotplaceRepository.delete(hotplace);
        deletePhotoAfterCommit(hotplace.getPhotoFile());
    }

    @Override
    public Resource getPhoto(Long id, Long ownerId) {
        Hotplace hotplace = findMine(id, ownerId);
        return photoStorage.load(hotplace.getPhotoFile()).orElseThrow(() -> new PhotoNotFoundException(id));
    }

    private Hotplace findMine(Long id, Long ownerId) {
        Hotplace hotplace = hotplaceRepository.findById(id).orElseThrow(() -> new HotplaceNotFoundException(id));
        if (!hotplace.isOwnedBy(ownerId)) {
            throw new HotplaceForbiddenException();
        }
        return hotplace;
    }

    private void checkVisitedDate(LocalDate visitedDate) {
        if (visitedDate.isAfter(LocalDate.now(clock))) {
            throw new InvalidVisitedDateException();
        }
    }

    /** 사진을 저장하고, 이 트랜잭션이 롤백되면 지워지도록 등록한다. */
    private String savePhoto(byte[] photo) {
        if (photo == null || photo.length == 0) {
            return null;
        }
        String fileName = photoStorage.save(photo);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    photoStorage.delete(fileName);
                }
            }
        });
        return fileName;
    }

    private void deletePhotoAfterCommit(String fileName) {
        if (fileName == null) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                photoStorage.delete(fileName);
            }
        });
    }
}
