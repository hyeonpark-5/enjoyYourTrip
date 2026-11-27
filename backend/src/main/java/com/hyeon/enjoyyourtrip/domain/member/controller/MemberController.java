package com.hyeon.enjoyyourtrip.domain.member.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hyeon.enjoyyourtrip.domain.member.dto.Request.MemberRoleUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.member.dto.Request.MemberSearchRequest;
import com.hyeon.enjoyyourtrip.domain.member.dto.Request.MemberUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.member.dto.Response.MemberResponse;
import com.hyeon.enjoyyourtrip.domain.member.service.MemberService;
import com.hyeon.enjoyyourtrip.global.common.PageResponse;
import com.hyeon.enjoyyourtrip.global.security.CurrentMemberId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Member", description = "회원 정보 (로그인·가입·비밀번호는 Keycloak)")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    /** 본인 또는 관리자 */
    private static final String SELF_OR_ADMIN = "hasRole('ADMIN') or @currentMember.is(#id)";

    private final MemberService memberService;

    @Operation(summary = "내 정보", description = "처음 로그인한 Keycloak 사용자는 이때 회원으로 등록된다.")
    @GetMapping("/me")
    public MemberResponse me(@CurrentMemberId Long memberId) {
        return memberService.getMember(memberId);
    }

    @Operation(summary = "회원 목록 (관리자)", description = "key=name|email, word=검색어, page=0부터, size, sort=id,desc")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<MemberResponse> search(
            @Valid @ModelAttribute MemberSearchRequest condition,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return memberService.search(condition, pageable);
    }

    @Operation(summary = "회원 상세 (본인 또는 관리자)")
    @PreAuthorize(SELF_OR_ADMIN)
    @GetMapping("/{id}")
    public MemberResponse get(@PathVariable Long id) {
        return memberService.getMember(id);
    }

    @Operation(summary = "이름 수정 (본인 또는 관리자)")
    @PreAuthorize(SELF_OR_ADMIN)
    @PatchMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberUpdateRequest request) {
        return memberService.update(id, request);
    }

    @Operation(summary = "권한 변경 (관리자)", description = "Keycloak realm 역할도 함께 바뀐다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public MemberResponse changeRole(@PathVariable Long id, @Valid @RequestBody MemberRoleUpdateRequest request) {
        return memberService.changeRole(id, request.role());
    }

    @Operation(summary = "회원 탈퇴 (본인 또는 관리자)", description = "Keycloak 계정을 비활성화하고 모든 세션을 끊는다.")
    @PreAuthorize(SELF_OR_ADMIN)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable Long id) {
        memberService.withdraw(id);
    }
}
