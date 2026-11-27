package com.hyeon.enjoyyourtrip.infrastructure.keycloak.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.hyeon.enjoyyourtrip.infrastructure.keycloak.client.KeycloakTokenApi;
import com.hyeon.enjoyyourtrip.infrastructure.keycloak.config.KeycloakProperties;
import com.hyeon.enjoyyourtrip.infrastructure.keycloak.dto.KeycloakToken;

import lombok.RequiredArgsConstructor;

/**
 * 서비스 계정 토큰(client_credentials)을 받아 만료 30초 전까지 재사용한다.
 */
@Component
@RequiredArgsConstructor
public class KeycloakAccessTokenProvider {

    private static final long EXPIRY_MARGIN_SECONDS = 30;

    private final KeycloakTokenApi tokenApi;
    private final KeycloakProperties properties;
    private final Clock clock;

    private String token;
    private Instant expiresAt = Instant.EPOCH;

    public synchronized String get() {
        Instant now = clock.instant();
        if (token == null || now.isAfter(expiresAt)) {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", properties.clientId());
            form.add("client_secret", properties.clientSecret());
            KeycloakToken response = tokenApi.token(properties.realm(), form);
            token = response.accessToken();
            expiresAt = now.plusSeconds(Math.max(0, response.expiresIn() - EXPIRY_MARGIN_SECONDS));
        }
        return token;
    }
}
