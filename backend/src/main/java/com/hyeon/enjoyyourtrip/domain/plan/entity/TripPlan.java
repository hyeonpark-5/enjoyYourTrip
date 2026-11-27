package com.hyeon.enjoyyourtrip.domain.plan.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.global.common.BaseTime;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 만든 여행 계획. 방문 순서대로 나열한 동선(stops)과 기간, 예상 경비, 메모를 가진다.
 */
@Getter
@Entity
@Table(name = "trip_plans")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripPlan extends BaseTime {

    private static final double EARTH_RADIUS_KM = 6371;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(nullable = false, length = 40)
    private String name;

    /** 원 단위. 0이면 미정 */
    @Column(nullable = false)
    private long budget;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 800)
    private String notes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "plan_stops", joinColumns = @JoinColumn(name = "plan_id"))
    @OrderColumn(name = "stop_order")
    private List<PlanStop> stops = new ArrayList<>();

    @Builder
    private TripPlan(Member owner, String name, long budget, LocalDate startDate, LocalDate endDate, String notes,
            List<PlanStop> stops) {
        this.owner = Objects.requireNonNull(owner);
        update(name, budget, startDate, endDate, notes, stops);
    }

    /** 계획 전체를 바꾼다(동선 포함). */
    public void update(String name, long budget, LocalDate startDate, LocalDate endDate, String notes,
            List<PlanStop> stops) {
        this.name = Objects.requireNonNull(name);
        this.budget = budget;
        this.startDate = Objects.requireNonNull(startDate);
        this.endDate = Objects.requireNonNull(endDate);
        this.notes = Objects.requireNonNull(notes);
        this.stops.clear();
        this.stops.addAll(stops);
    }

    public List<PlanStop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    public boolean isOwnedBy(Long memberId) {
        return owner.getId().equals(memberId);
    }

    /** 방문 순서대로 이은 총 직선거리(km, 하버사인). 장소가 둘 미만이면 0 */
    public double distanceKm() {
        double total = 0;
        for (int i = 1; i < stops.size(); i++) {
            PlanStop from = stops.get(i - 1);
            PlanStop to = stops.get(i);
            double lat1 = Math.toRadians(from.getLat());
            double lat2 = Math.toRadians(to.getLat());
            double deltaLat = lat2 - lat1;
            double deltaLng = Math.toRadians(to.getLng() - from.getLng());
            double haversine = Math.pow(Math.sin(deltaLat / 2), 2)
                    + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLng / 2), 2);
            total += EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        }
        return total;
    }
}
