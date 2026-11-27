package com.hyeon.enjoyyourtrip.infrastructure.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hyeon.enjoyyourtrip.infrastructure.keycloak.dto.KeycloakToken;

import tools.jackson.databind.json.JsonMapper;

class KeycloakTokenJsonTest {

    @Test
    @DisplayName("토큰 엔드포인트 응답(snake_case)을 KeycloakToken으로 읽는다")
    void readsSnakeCaseToken() {
        KeycloakToken token = JsonMapper.builder().build().readValue(
                "{\"access_token\":\"abc\",\"expires_in\":300,\"token_type\":\"Bearer\"}", KeycloakToken.class);

        assertThat(token.accessToken()).isEqualTo("abc");
        assertThat(token.expiresIn()).isEqualTo(300);
    }
}
