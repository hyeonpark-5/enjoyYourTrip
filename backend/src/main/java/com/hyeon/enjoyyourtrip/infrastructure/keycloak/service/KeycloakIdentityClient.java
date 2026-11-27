package com.hyeon.enjoyyourtrip.infrastructure.keycloak.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.hyeon.enjoyyourtrip.domain.member.entity.Role;
import com.hyeon.enjoyyourtrip.domain.member.exception.IdentityProviderUnavailableException;
import com.hyeon.enjoyyourtrip.domain.member.service.IdentityProviderClient;
import com.hyeon.enjoyyourtrip.infrastructure.keycloak.client.KeycloakAdminApi;
import com.hyeon.enjoyyourtrip.infrastructure.keycloak.config.KeycloakProperties;
import com.hyeon.enjoyyourtrip.infrastructure.keycloak.dto.KeycloakRole;
import com.hyeon.enjoyyourtrip.infrastructure.keycloak.dto.KeycloakUser;

import lombok.RequiredArgsConstructor;

/**
 * Keycloak Admin API 구현체. 외부 예외(통신·권한 오류)를 도메인 예외(IdentityProviderUnavailableException)로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class KeycloakIdentityClient implements IdentityProviderClient {

    private final KeycloakAdminApi adminApi;
    private final KeycloakProperties properties;

    @Override
    public void disableUser(String keycloakId) {
        call("사용자 비활성화", () -> {
            adminApi.updateUser(realm(), keycloakId, Map.of("enabled", false));
            adminApi.logout(realm(), keycloakId);
            return null;
        });
    }

    @Override
    public void setAdmin(String keycloakId, boolean admin) {
        call("역할 변경", () -> {
            List<KeycloakRole> roles = List.of(adminApi.getRealmRole(realm(), Role.ADMIN.name()));
            if (admin) {
                adminApi.addRealmRoles(realm(), keycloakId, roles);
            } else {
                adminApi.removeRealmRoles(realm(), keycloakId, roles);
            }
            return null;
        });
    }

    @Override
    public Optional<String> findUserIdByEmail(String email) {
        return call("사용자 조회", () -> adminApi.findUsers(realm(), email, true).stream()
                .map(KeycloakUser::id)
                .findFirst());
    }

    @Override
    public String createUser(String email, String password, String name) {
        return call("사용자 생성", () -> {
            KeycloakUser user = new KeycloakUser(null, email, email, name, true, true,
                    List.of(new KeycloakUser.Credential("password", password, false)));
            ResponseEntity<Void> response = adminApi.createUser(realm(), user);
            URI location = response.getHeaders().getLocation();
            if (location == null) {
                throw new IdentityProviderUnavailableException("생성된 사용자 id를 알 수 없음", null);
            }
            String path = location.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        });
    }

    private String realm() {
        return properties.realm();
    }

    private <T> T call(String action, Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException e) {
            throw new IdentityProviderUnavailableException(action + " 실패", e);
        }
    }
}
