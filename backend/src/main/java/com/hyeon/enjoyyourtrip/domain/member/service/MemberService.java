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

    /** 화면에 보이는 이름을 바꾼다. */
    MemberResponse update(Long id, MemberUpdateRequest request);

    /** 우리 DB와 Keycloak realm 역할을 함께 바꾼다. 대상 회원의 토큰에는 다음 갱신부터 반영된다. */
    MemberResponse changeRole(Long id, Role role);

    /** 소프트 삭제하고 Keycloak 계정을 비활성화(모든 세션 종료)한다. */
    void withdraw(Long id);
}
