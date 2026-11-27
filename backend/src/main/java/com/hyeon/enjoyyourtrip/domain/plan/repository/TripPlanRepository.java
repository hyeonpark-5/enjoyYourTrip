package com.hyeon.enjoyyourtrip.domain.plan.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeon.enjoyyourtrip.domain.plan.entity.TripPlan;

/** 목록·상세 모두 동선(stops)을 함께 쓰므로 한 번에 가져온다(N+1 방지). */
public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {

    @EntityGraph(attributePaths = "stops")
    List<TripPlan> findByOwnerIdOrderByIdDesc(Long ownerId);

    @EntityGraph(attributePaths = "stops")
    Optional<TripPlan> findWithStopsById(Long id);
}
