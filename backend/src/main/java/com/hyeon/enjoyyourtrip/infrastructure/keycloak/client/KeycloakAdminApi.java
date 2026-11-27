package com.hyeon.enjoyyourtrip.infrastructure.keycloak.client;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import com.hyeon.enjoyyourtrip.infrastructure.keycloak.dto.KeycloakRole;
import com.hyeon.enjoyyourtrip.infrastructure.keycloak.dto.KeycloakUser;

/**
 * Keycloak Admin REST API (필요한 것만). Authorization 헤더는 KeycloakConfig의 인터셉터가 붙인다.
 * 서비스 계정에는 realm-management의 manage-users, view-users, view-realm 역할이 있어야 한다(realm JSON에 설정).
 */
@HttpExchange("/admin/realms/{realm}")
public interface KeycloakAdminApi {

    @GetExchange("/users")
    List<KeycloakUser> findUsers(@PathVariable String realm, @RequestParam String email, @RequestParam boolean exact);

    /** 201 Created, Location 헤더 끝이 새 사용자 id */
    @PostExchange("/users")
    ResponseEntity<Void> createUser(@PathVariable String realm, @RequestBody KeycloakUser user);

    @PutExchange("/users/{id}")
    void updateUser(@PathVariable String realm, @PathVariable String id, @RequestBody Map<String, Object> changes);

    /** 사용자의 모든 세션 종료 (발급된 Refresh Token 무효) */
    @PostExchange("/users/{id}/logout")
    void logout(@PathVariable String realm, @PathVariable String id);

    @GetExchange("/roles/{role}")
    KeycloakRole getRealmRole(@PathVariable String realm, @PathVariable String role);

    @PostExchange("/users/{id}/role-mappings/realm")
    void addRealmRoles(@PathVariable String realm, @PathVariable String id, @RequestBody List<KeycloakRole> roles);

    @HttpExchange(method = "DELETE", url = "/users/{id}/role-mappings/realm")
    void removeRealmRoles(@PathVariable String realm, @PathVariable String id, @RequestBody List<KeycloakRole> roles);
}
