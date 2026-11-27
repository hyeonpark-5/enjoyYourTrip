package com.hyeon.enjoyyourtrip.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.hyeon.enjoyyourtrip.domain.member.service.MemberSyncService;
import com.hyeon.enjoyyourtrip.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * 현재 요청의 로그인 회원 id. Keycloak 토큰(sub)으로 회원을 찾고(없으면 만들고) 한 요청 안에서는 결과를 재사용한다.
 */
@Component
@RequiredArgsConstructor
public class CurrentMember {

    private static final String CACHE_KEY = CurrentMember.class.getName() + ".id";

    private final MemberSyncService memberSyncService;

    /** @return 로그인 회원 id, 로그인하지 않았으면 null */
    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        RequestAttributes request = RequestContextHolder.getRequestAttributes();
        if (request != null && request.getAttribute(CACHE_KEY, RequestAttributes.SCOPE_REQUEST) instanceof Long cached) {
            return cached;
        }
        Long id = memberSyncService.sync(KeycloakClaims.toLoginUser(jwt));
        if (request != null) {
            request.setAttribute(CACHE_KEY, id, RequestAttributes.SCOPE_REQUEST);
        }
        return id;
    }

    /** @PreAuthorize("... or @currentMember.is(#id)") 처럼 본인 확인에 쓴다. */
    public boolean is(Long memberId) {
        try {
            return memberId != null && memberId.equals(id());
        } catch (BusinessException e) {
            return false; // 탈퇴 회원 등은 본인 권한 없음(403)으로 처리
        }
    }
}
