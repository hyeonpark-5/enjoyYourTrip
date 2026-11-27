package com.hyeon.enjoyyourtrip.domain.plan.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 여행 동선의 한 장소(값 객체). 순서는 TripPlan의 목록 순서(plan_stops.stop_order)로 정해진다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanStop {

    @Column(nullable = false, length = 40)
    private String placeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Column(nullable = false, length = 5)
    private String contentTypeId;

    public PlanStop(String placeId, String name, String address, double lat, double lng, String contentTypeId) {
        this.placeId = Objects.requireNonNull(placeId);
        this.name = Objects.requireNonNull(name);
        this.address = Objects.requireNonNull(address);
        this.lat = lat;
        this.lng = lng;
        this.contentTypeId = Objects.requireNonNull(contentTypeId);
    }
}
