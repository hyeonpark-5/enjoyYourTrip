package com.hyeon.enjoyyourtrip.domain.member.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hyeon.enjoyyourtrip.domain.auth.service.AuthService;
import com.hyeon.enjoyyourtrip.domain.member.dto.Request.MemberRoleUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.member.dto.Request.MemberSearchRequest;
import com.hyeon.enjoyyourtrip.domain.member.dto.Request.MemberUpdateRequest;
import com.hyeon.enjoyyourtrip.domain.member.dto.Response.MemberResponse;
import com.hyeon.enjoyyourtrip.domain.member.service.MemberService;
import com.hyeon.enjoyyourtrip.global.common.PageResponse;
import com.hyeon.enjoyyourtrip.global.security.RefreshTokenCookieProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Member", description = "회원 정보")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    /** 본인(토큰의 sub == 경로의 id) 또는 관리자 */
    private static final String SELF_OR_ADMIN = "hasRole('ADMIN') or #id.toString() == authentication.name";

    private final MemberService memberService;
    private final AuthService authService;
    private final RefreshTokenCookieProvider cookieProvider;

    @Operation(summary = "내 정보")
    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal Jwt jwt) {
        return memberService.getMember(memberId(jwt));
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

    @Operation(summary = "회원 정보 수정 (본인 또는 관리자)", description = "password는 바꿀 때만 보낸다.")
    @PreAuthorize(SELF_OR_ADMIN)
    @PatchMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberUpdateRequest request) {
        return memberService.update(id, request);
    }

    @Operation(summary = "권한 변경 (관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public MemberResponse changeRole(@PathVariable Long id, @Valid @RequestBody MemberRoleUpdateRequest request) {
        return memberService.changeRole(id, request.role());
    }

    @Operation(summary = "회원 탈퇴 (본인 또는 관리자)", description = "본인이 탈퇴하면 현재 토큰도 함께 무효화된다.")
    @PreAuthorize(SELF_OR_ADMIN)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        memberService.withdraw(id);
        if (id.equals(memberId(jwt))) {
            authService.logout(jwt.getId(), jwt.getExpiresAt(), refreshToken);
            response.addHeader(HttpHeaders.SET_COOKIE, cookieProvider.clear().toString());
        }
    }

    private static Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
