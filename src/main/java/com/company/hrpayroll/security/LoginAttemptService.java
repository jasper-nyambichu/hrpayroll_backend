// security/LoginAttemptService.java
package com.company.hrpayroll.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed login throttling. Moved off Caffeine so lockout state is
 * shared across instances the moment this app ever runs on more than one
 * node — a single VPS didn't strictly need this, but Redis is now part of
 * the infrastructure regardless, and keeping rate-limit state here (rather
 * than in-process) means a backend restart never silently resets someone's
 * lockout window.
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private static final String ATTEMPTS_PREFIX = "login:attempts:";
    private static final String LOCKED_PREFIX = "login:locked:";

    private final StringRedisTemplate redisTemplate;

    public boolean isLocked(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCKED_PREFIX + key));
    }

    public void recordFailure(String key) {
        String attemptsKey = ATTEMPTS_PREFIX + key;

        Long count = redisTemplate.opsForValue().increment(attemptsKey);
        if (count != null && count == 1L) {
            // First failure in this window — set the TTL so a stale
            // counter never lingers past the lockout duration.
            redisTemplate.expire(attemptsKey, LOCKOUT_DURATION);
        }

        if (count != null && count >= MAX_ATTEMPTS) {
            redisTemplate.opsForValue().set(LOCKED_PREFIX + key, "1", LOCKOUT_DURATION);
            redisTemplate.delete(attemptsKey);
        }
    }

    public void recordSuccess(String key) {
        redisTemplate.delete(ATTEMPTS_PREFIX + key);
        redisTemplate.delete(LOCKED_PREFIX + key);
    }
}