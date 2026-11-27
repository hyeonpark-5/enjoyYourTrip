package com.hyeon.enjoyyourtrip.global.security;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 로그아웃된 Access Token의 jti를 남은 만료 시간만큼 Redis에 보관한다.
 * JwtDecoder의 검증 단계(BlacklistTokenValidator)에서 조회해 거부한다.
 */
@Component
@RequiredArgsConstructor
public class AccessTokenBlacklist {

    private static final String KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void add(String jti, Duration remaining) {
        if (jti == null || remaining.isNegative() || remaining.isZero()) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", remaining);
    }

    public boolean contains(String jti) {
        return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
