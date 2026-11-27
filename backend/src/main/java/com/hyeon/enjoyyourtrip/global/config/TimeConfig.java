package com.hyeon.enjoyyourtrip.global.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 현재 시각은 항상 이 Clock 빈에서 얻는다. (LocalDateTime.now() 직접 호출 금지)
 * 테스트에서는 고정된 Clock으로 바꿔 끼워 시간에 의존하는 로직을 검증한다.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
