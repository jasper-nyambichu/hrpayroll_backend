// security/LoginAttemptService.java
package com.company.hrpayroll.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory login throttling. A single VPS instance needs no shared store —
 * Caffeine keeps this simple until a second instance ever exists, at which
 * point this moves to Redis so lockout state is shared across nodes.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final Cache<String, AtomicInteger> attempts = Caffeine.newBuilder()
            .expireAfterWrite(LOCKOUT_DURATION)
            .build();

    private final Cache<String, Boolean> lockedOut = Caffeine.newBuilder()
            .expireAfterWrite(LOCKOUT_DURATION)
            .build();

    public boolean isLocked(String key) {
        return lockedOut.getIfPresent(key) != null;
    }

    public void recordFailure(String key) {
        AtomicInteger count = attempts.get(key, k -> new AtomicInteger(0));
        if (count.incrementAndGet() >= MAX_ATTEMPTS) {
            lockedOut.put(key, Boolean.TRUE);
            attempts.invalidate(key);
        }
    }

    public void recordSuccess(String key) {
        attempts.invalidate(key);
        lockedOut.invalidate(key);
    }
}
