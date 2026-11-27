package com.hyeon.enjoyyourtrip.domain.member.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeon.enjoyyourtrip.domain.member.entity.Member;

/**
 * 탈퇴 회원은 조회 대상에서 빠지도록 "...AndDeletedAtIsNull" 메서드를 쓴다.
 * findByKeycloakId·existsByEmail은 탈퇴 회원까지 포함한다(탈퇴 여부를 직접 판단하기 위함).
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    Optional<Member> findByEmailAndDeletedAtIsNull(String email);

    Page<Member> findByDeletedAtIsNull(Pageable pageable);

    Page<Member> findByNameContainingAndDeletedAtIsNull(String name, Pageable pageable);

    Page<Member> findByEmailContainingAndDeletedAtIsNull(String email, Pageable pageable);
}
