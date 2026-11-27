package com.hyeon.enjoyyourtrip.domain.member.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hyeon.enjoyyourtrip.domain.member.entity.Member;

/**
 * 탈퇴 회원은 조회 대상에서 빠지도록 "...AndDeletedAtIsNull" 메서드를 쓴다.
 * existsByEmail만 탈퇴 회원까지 포함한다(탈퇴한 이메일 재가입 방지).
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    Optional<Member> findByEmailAndDeletedAtIsNull(String email);

    Page<Member> findByDeletedAtIsNull(Pageable pageable);

    Page<Member> findByNameContainingAndDeletedAtIsNull(String name, Pageable pageable);

    Page<Member> findByEmailContainingAndDeletedAtIsNull(String email, Pageable pageable);
}
