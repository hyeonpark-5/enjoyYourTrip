package com.hyeon.enjoyyourtrip.domain.auth.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Refresh Token 저장소(Redis). 원문 대신 SHA-256 해시를 키로 저장해 Redis가 유출돼도 토큰을 쓸 수 없게 한다.
 *
 * <pre>
 * auth:refresh-token:{hash}       → 회원 id          (TTL = refresh 만료)
 * auth:member-refresh:{memberId}  → {hash, hash...}  (회원의 모든 토큰 일괄 폐기용)
 * </pre>
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String TOKEN_KEY = "auth:refresh-token:";
    private static final String MEMBER_KEY = "auth:member-refresh:";

    private final StringRedisTemplate redisTemplate;

    public void save(String refreshToken, Long memberId, Duration ttl) {
        String hash = hash(refreshToken);
        String memberKey = MEMBER_KEY + memberId;
        redisTemplate.opsForValue().set(TOKEN_KEY + hash, String.valueOf(memberId), ttl);
        redisTemplate.opsForSet().add(memberKey, hash);
        redisTemplate.expire(memberKey, ttl);
    }

    public Optional<Long> findMemberId(String refreshToken) {
        String memberId = redisTemplate.opsForValue().get(TOKEN_KEY + hash(refreshToken));
        return Optional.ofNullable(memberId).map(Long::valueOf);
    }

    public void delete(String refreshToken) {
        String hash = hash(refreshToken);
        String memberId = redisTemplate.opsForValue().getAndDelete(TOKEN_KEY + hash);
        if (memberId != null) {
            redisTemplate.opsForSet().remove(MEMBER_KEY + memberId, hash);
        }
    }

    /** 비밀번호 재설정·탈퇴 시 모든 기기의 로그인을 끊는다. */
    public void deleteAllByMemberId(Long memberId) {
        String memberKey = MEMBER_KEY + memberId;
        Set<String> hashes = redisTemplate.opsForSet().members(memberKey);
        if (hashes != null && !hashes.isEmpty()) {
            redisTemplate.delete(hashes.stream().map(hash -> TOKEN_KEY + hash).toList());
        }
        redisTemplate.delete(memberKey);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
