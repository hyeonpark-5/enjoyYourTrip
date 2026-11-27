package com.hyeon.enjoyyourtrip.global.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.jwt.Jwt;

import com.hyeon.enjoyyourtrip.domain.member.entity.Role;
import com.hyeon.enjoyyourtrip.domain.member.service.LoginUser;

/**
 * Keycloak Access Token 클레임 읽기.
 * 역할은 realm_access.roles 에 들어 있다: {"realm_access": {"roles": ["USER", "ADMIN", "offline_access", ...]}}
 */
public final class KeycloakClaims {

    private KeycloakClaims() {
    }

    public static List<String> realmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles) {
            return roles.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public static LoginUser toLoginUser(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = jwt.getClaimAsString("preferred_username");
        }
        Role role = realmRoles(jwt).contains(Role.ADMIN.name()) ? Role.ADMIN : Role.USER;
        return new LoginUser(jwt.getSubject(), jwt.getClaimAsString("email"), name, role);
    }
}
