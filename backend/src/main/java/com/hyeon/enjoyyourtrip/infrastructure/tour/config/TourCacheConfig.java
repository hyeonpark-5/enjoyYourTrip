package com.hyeon.enjoyyourtrip.infrastructure.tour.config;

import java.time.Duration;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TourAPI 응답 캐시(Redis). 관광 정보는 자주 바뀌지 않으므로 호출 수(일일 한도)를 줄이고 응답을 빠르게 한다.
 * 목록은 spring.cache.redis.time-to-live(기본 1시간), 상세는 하루 보관한다.
 */
@Configuration
public class TourCacheConfig {

    public static final String AREA = "tour:area";
    public static final String KEYWORD = "tour:keyword";
    public static final String NEARBY = "tour:nearby";
    public static final String DETAIL = "tour:detail";

    @Bean
    public RedisCacheManagerBuilderCustomizer tourCacheCustomizer() {
        return builder -> builder.withCacheConfiguration(DETAIL, builder.cacheDefaults().entryTtl(Duration.ofDays(1)));
    }
}
