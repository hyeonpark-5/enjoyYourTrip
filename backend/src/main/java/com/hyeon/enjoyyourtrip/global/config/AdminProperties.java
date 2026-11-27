package com.hyeon.enjoyyourtrip.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** app.admin.* : 최초 관리자 계정. email이 비어 있으면 생성하지 않는다. */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password, String name) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
