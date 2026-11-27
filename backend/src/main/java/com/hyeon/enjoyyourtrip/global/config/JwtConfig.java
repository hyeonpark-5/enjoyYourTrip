package com.hyeon.enjoyyourtrip.global.config;

import java.util.Collection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import com.hyeon.enjoyyourtrip.global.security.KeycloakClaims;

/**
 * Keycloak이 발급한 Access Token 해석.
 * 토큰 검증기(JwtDecoder)는 application.yaml의 spring.security.oauth2.resourceserver.jwt.*
 * (issuer-uri, jwk-set-uri, audiences)로 Spring Boot가 만든다.
 */
@Configuration
public class JwtConfig {

    /** realm_access.roles → ROLE_USER / ROLE_ADMIN 권한 */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = KeycloakClaims.realmRoles(jwt).stream()
                    .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            return authorities;
        });
        return converter;
    }
}
