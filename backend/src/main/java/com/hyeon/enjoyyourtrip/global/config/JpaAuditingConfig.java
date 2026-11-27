package com.hyeon.enjoyyourtrip.global.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseTime의 createdAt/updatedAt을 채운다. 시각은 Clock 빈을 따른다.
 * 메인 클래스가 아닌 별도 설정 클래스에 두어야 @WebMvcTest 같은 슬라이스 테스트가 JPA 없이 뜬다.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(LocalDateTime.now(clock));
    }
}
