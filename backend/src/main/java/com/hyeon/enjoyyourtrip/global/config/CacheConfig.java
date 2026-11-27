package com.hyeon.enjoyyourtrip.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * @Cacheable 활성화. 저장소는 spring.cache.type(redis)을 따르며, 캐시별 TTL은 각 기능의 설정 클래스에 둔다.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
