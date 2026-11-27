package com.hyeon.enjoyyourtrip.domain.member.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hyeon.enjoyyourtrip.domain.auth.repository.RefreshTokenRepository;
import com.hyeon.enjoyyourtrip.domain.member.dto.Request.MemberSearchRequest;
import com.hyeon.enjoyyourtrip.domain.member.dto.Request.MemberUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.member.dto.Response.MemberResponse;
import com.hyeon.enjoyyourtrip.domain.member.entity.Member;
import com.hyeon.enjoyyourtrip.domain.member.entity.Role;
import com.hyeon.enjoyyourtrip.domain.member.exception.MemberNotFoundException;
import com.hyeon.enjoyyourtrip.domain.member.mapper.MemberMapper;
import com.hyeon.enjoyyourtrip.domain.member.repository.MemberRepository;
import com.hyeon.enjoyyourtrip.global.common.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * 회원 조회·수정·탈퇴. 접근 권한(본인 또는 관리자)은 컨트롤러의 @PreAuthorize에서 검사한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public MemberResponse getMember(Long id) {
        return memberMapper.toResponse(findActive(id));
    }

    public PageResponse<MemberResponse> search(MemberSearchRequest condition, Pageable pageable) {
        Page<Member> page;
        if (!condition.hasCondition()) {
            page = memberRepository.findByDeletedAtIsNull(pageable);
        } else if ("email".equals(condition.key())) {
            page = memberRepository.findByEmailContainingAndDeletedAtIsNull(condition.word().trim(), pageable);
        } else {
            page = memberRepository.findByNameContainingAndDeletedAtIsNull(condition.word().trim(), pageable);
        }
        return PageResponse.of(page, memberMapper::toResponse);
    }

    @Transactional
    public MemberResponse update(Long id, MemberUpdateRequest request) {
        Member member = findActive(id);
        member.changeName(request.name().trim());
        if (request.password() != null) {
            member.changePassword(passwordEncoder.encode(request.password()));
        }
        return memberMapper.toResponse(member);
    }

    @Transactional
    public MemberResponse changeRole(Long id, Role role) {
        Member member = findActive(id);
        member.changeRole(role);
        return memberMapper.toResponse(member);
    }

    /** 소프트 삭제 후 모든 기기의 Refresh Token을 폐기한다. */
    @Transactional
    public void withdraw(Long id) {
        Member member = findActive(id);
        member.withdraw(LocalDateTime.now(clock));
        refreshTokenRepository.deleteAllByMemberId(id);
    }

    private Member findActive(Long id) {
        return memberRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new MemberNotFoundException(id));
    }
}
