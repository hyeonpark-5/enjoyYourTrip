package com.hyeon.enjoyyourtrip.infrastructure.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Admin API의 RoleRepresentation 중 역할 연결에 필요한 필드만 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakRole(String id, String name) {
}
