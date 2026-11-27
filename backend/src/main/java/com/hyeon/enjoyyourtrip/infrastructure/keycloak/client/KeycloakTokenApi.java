package com.hyeon.enjoyyourtrip.infrastructure.keycloak.client;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import com.hyeon.enjoyyourtrip.infrastructure.keycloak.dto.KeycloakToken;

/** Keycloak 토큰 엔드포인트. Admin API 호출에 쓸 서비스 계정 토큰을 받는다. */
@HttpExchange
public interface KeycloakTokenApi {

    @PostExchange(url = "/realms/{realm}/protocol/openid-connect/token",
            contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    KeycloakToken token(@PathVariable String realm, @RequestBody MultiValueMap<String, String> form);
}
