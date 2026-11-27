package com.hyeon.enjoyyourtrip.domain.hotplace.entity;

import java.time.LocalDate;
import java.util.Objects;

import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.global.common.BaseTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 다녀온 장소 기록. 장소(place*) 필드는 등록 시점의 TourAPI 정보를 복사한 값이라 이후 바뀌지 않는다.
 */
@Getter
@Entity
@Table(name = "hotplaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hotplace extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(nullable = false, length = 20)
    private String placeId;

    @Column(nullable = false, length = 200)
    private String placeName;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Column(nullable = false, length = 10)
    private String contentTypeId;

    /** TourAPI 대표 이미지 URL (직접 올린 사진이 없을 때 쓴다). 없으면 빈 문자열 */
    @Column(nullable = false, length = 500)
    private String placeImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlaceType placeType;

    @Column(nullable = false)
    private LocalDate visitedDate;

    @Column(nullable = false, length = 500)
    private String description;

    /** 서버에 저장된 사진 파일명. 없으면 null */
    @Column(length = 64)
    private String photoFile;

    @Builder
    private Hotplace(Member owner, String placeId, String placeName, String address, double lat, double lng,
            String contentTypeId, String placeImage, PlaceType placeType, LocalDate visitedDate, String description,
            String photoFile) {
        this.owner = Objects.requireNonNull(owner);
        this.placeId = Objects.requireNonNull(placeId);
        this.placeName = Objects.requireNonNull(placeName);
        this.address = Objects.requireNonNull(address);
        this.lat = lat;
        this.lng = lng;
        this.contentTypeId = Objects.requireNonNull(contentTypeId);
        this.placeImage = Objects.requireNonNull(placeImage);
        this.placeType = Objects.requireNonNull(placeType);
        this.visitedDate = Objects.requireNonNull(visitedDate);
        this.description = Objects.requireNonNull(description);
        this.photoFile = photoFile;
    }

    public void edit(PlaceType placeType, LocalDate visitedDate, String description) {
        this.placeType = Objects.requireNonNull(placeType);
        this.visitedDate = Objects.requireNonNull(visitedDate);
        this.description = Objects.requireNonNull(description);
    }

    /** @param photoFile 새 사진 파일명, 사진을 지우면 null */
    public void changePhoto(String photoFile) {
        this.photoFile = photoFile;
    }

    public boolean isOwnedBy(Long memberId) {
        return owner.getId().equals(memberId);
    }

    public boolean hasPhoto() {
        return photoFile != null;
    }
}
