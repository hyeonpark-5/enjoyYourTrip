package com.hyeon.enjoyyourtrip.infrastructure.keycloak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.keycloak.* — Admin API를 호출할 서비스 계정(enjoyyourtrip-backend). 주소는 spring.http.serviceclient.keycloak-*.base-url.
 */
@ConfigurationProperties(prefix = "app.keycloak")
public record KeycloakProperties(String realm, String clientId, String clientSecret) {
}
