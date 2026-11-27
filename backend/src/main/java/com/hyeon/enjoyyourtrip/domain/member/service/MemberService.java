package com.hyeon.enjoyyourtrip.domain.member.service;

import org.springframework.data.domain.Pageable;

import com.hyeon.enjoyyourtrip.domain.member.dto.Request.MemberSearchRequest;
import com.hyeon.enjoyyourtrip.domain.member.dto.Request.MemberUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.member.dto.Response.MemberResponse;
import com.hyeon.enjoyyourtrip.domain.member.entity.Role;
import com.hyeon.enjoyyourtrip.global.common.PageResponse;

/**
 * 회원 조회·수정·탈퇴. 접근 권한(본인 또는 관리자)은 컨트롤러의 @PreAuthorize에서 검사한다.
 */
public interface MemberService {

    MemberResponse getMember(Long id);

    /** 탈퇴하지 않은 회원을 이름 또는 이메일로 검색한다. */
    PageResponse<MemberResponse> search(MemberSearchRequest condition, Pageable pageable);

    /** 이름을 바꾸고, 비밀번호가 주어지면 함께 바꾼다. */
    MemberResponse update(Long id, MemberUpdateRequest request);

    MemberResponse changeRole(Long id, Role role);

    /** 소프트 삭제 후 모든 기기의 Refresh Token을 폐기한다. */
    void withdraw(Long id);
}
