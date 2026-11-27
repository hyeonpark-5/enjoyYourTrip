package com.hyeon.enjoyyourtrip.infrastructure.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 토큰 엔드포인트 응답 (client_credentials) */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakToken(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") long expiresIn) {
}
