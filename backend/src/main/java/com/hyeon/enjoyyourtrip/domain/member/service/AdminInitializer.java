package com.hyeon.enjoyyourtrip.domain.member.service;

import java.util.Locale;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.hyeon.enjoyyourtrip.global.config.AdminProperties;
import com.hyeon.enjoyyourtrip.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서버가 뜬 뒤 .env의 ADMIN_EMAIL/ADMIN_PASSWORD로 Keycloak에 관리자 계정을 만들고 ADMIN 역할을 준다(이미 있으면 역할만 확인).
 * 우리 DB의 회원 정보는 그 계정으로 처음 로그인할 때 만들어진다.
 * Keycloak이 아직 안 떠 있으면 경고만 남기고 넘어간다(서버 재시작 시 다시 시도).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
public class AdminInitializer {

    private final AdminProperties properties;
    private final IdentityProviderClient identityProviderClient;

    @EventListener(ApplicationReadyEvent.class)
    public void createAdmin() {
        if (!properties.isConfigured()) {
            return;
        }
        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        try {
            String keycloakId = identityProviderClient.findUserIdByEmail(email)
                    .orElseGet(() -> {
                        log.info("Keycloak에 관리자 계정을 만듭니다: {}", email);
                        return identityProviderClient.createUser(email, properties.password(), properties.name());
                    });
            identityProviderClient.setAdmin(keycloakId, true);
        } catch (BusinessException e) {
            log.warn("관리자 계정을 준비하지 못했습니다. Keycloak 실행과 KEYCLOAK_* 설정을 확인하세요. ({})", e.getMessage());
        }
    }
}
