package com.hyeon.enjoyyourtrip.domain.hotplace.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeon.enjoyyourtrip.domain.hotplace.entity.Hotplace;

public interface HotplaceRepository extends JpaRepository<Hotplace, Long> {

    /** 내 핫플레이스 최신 등록순 */
    List<Hotplace> findByOwnerIdOrderByIdDesc(Long ownerId);

    boolean existsByOwnerIdAndPlaceIdAndVisitedDate(Long ownerId, String placeId, LocalDate visitedDate);

    /** 수정 시 자기 자신을 뺀 중복 확인 */
    boolean existsByOwnerIdAndPlaceIdAndVisitedDateAndIdNot(
            Long ownerId, String placeId, LocalDate visitedDate, Long id);
}
