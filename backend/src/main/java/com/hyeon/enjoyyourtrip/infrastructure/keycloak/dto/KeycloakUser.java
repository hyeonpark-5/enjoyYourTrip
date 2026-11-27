package com.hyeon.enjoyyourtrip.infrastructure.keycloak.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Admin API의 UserRepresentation 중 쓰는 필드만 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakUser(
        String id,
        String username,
        String email,
        String firstName,
        Boolean enabled,
        Boolean emailVerified,
        List<Credential> credentials) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Credential(String type, String value, Boolean temporary) {
    }
}
