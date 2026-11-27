package com.hyeon.enjoyyourtrip.domain.member.service;

import java.util.Optional;

/**
 * 인증 서버(Keycloak)의 사용자 관리 도메인 인터페이스(DIP). 구현체는 infrastructure.keycloak.service.KeycloakIdentityClient.
 * 호출 실패는 IdentityProviderUnavailableException(502)으로 던진다.
 */
public interface IdentityProviderClient {

    /** 로그인을 막고 모든 세션을 끊는다 (회원 탈퇴). */
    void disableUser(String keycloakId);

    /** ADMIN realm 역할을 주거나 뺀다. */
    void setAdmin(String keycloakId, boolean admin);

    Optional<String> findUserIdByEmail(String email);

    /** @return 만든 사용자의 Keycloak id */
    String createUser(String email, String password, String name);
}
