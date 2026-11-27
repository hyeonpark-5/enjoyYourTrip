package com.hyeon.enjoyyourtrip.domain.member.service;

import com.hyeon.enjoyyourtrip.domain.member.entity.Role;

/**
 * Keycloak Access Token에서 읽은 로그인 사용자 정보 (웹·보안 의존성 없는 도메인 값).
 *
 * @param keycloakId 토큰의 sub
 * @param name       토큰의 name(이름 + 성), 없으면 preferred_username
 * @param role       realm 역할에 ADMIN이 있으면 ADMIN, 아니면 USER
 */
public record LoginUser(String keycloakId, String email, String name, Role role) {
}
