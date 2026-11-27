package com.hyeon.enjoyyourtrip.infrastructure.keycloak.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

import com.hyeon.enjoyyourtrip.infrastructure.keycloak.client.KeycloakAdminApi;
import com.hyeon.enjoyyourtrip.infrastructure.keycloak.client.KeycloakTokenApi;
import com.hyeon.enjoyyourtrip.infrastructure.keycloak.service.KeycloakAccessTokenProvider;

/**
 * Keycloak HTTP 클라이언트 등록. 주소는 application.yaml의 spring.http.serviceclient.keycloak-token / keycloak-admin.
 * Admin API 그룹에는 서비스 계정 토큰을 붙인다(토큰 발급은 별도 그룹이라 순환하지 않는다).
 */
@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
@ImportHttpServices(group = KeycloakConfig.TOKEN_GROUP, types = KeycloakTokenApi.class)
@ImportHttpServices(group = KeycloakConfig.ADMIN_GROUP, types = KeycloakAdminApi.class)
public class KeycloakConfig {

    public static final String TOKEN_GROUP = "keycloak-token";
    public static final String ADMIN_GROUP = "keycloak-admin";

    @Bean
    public RestClientHttpServiceGroupConfigurer keycloakAdminGroupConfigurer(
            ObjectProvider<KeycloakAccessTokenProvider> tokenProvider) {
        return groups -> groups.filterByName(ADMIN_GROUP).forEachClient((group, builder) ->
                builder.requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokenProvider.getObject().get());
                    return execution.execute(request, body);
                }));
    }
}
