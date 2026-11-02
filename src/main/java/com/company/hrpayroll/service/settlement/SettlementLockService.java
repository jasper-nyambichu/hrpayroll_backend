// service/settlement/SettlementLockService.java
package com.company.hrpayroll.service.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Prevents two agent payment calculations from executing concurrently, system-wide.
 *
 * A robust per-scope overlap check would need to reason about arbitrary
 * intersections between branch, agent, department, and "all employees"
 * scopes. Instead, this takes the simpler, safer position that only one
 * calculation may execute at a time, system-wide — a legitimate constraint
 * for money, not just an implementation shortcut.
 *
 * pg_try_advisory_xact_lock is transaction-scoped: released automatically
 * when the calling @Transactional method commits or rolls back, so there
 * is no manual unlock path to forget.
 */
@Component
@RequiredArgsConstructor
public class SettlementLockService {

    // Arbitrary fixed key identifying "the calculation lock" — any constant
    // works, it only needs to be the same value on every call.
    private static final long SETTLEMENT_LOCK_KEY = 7_224_991L;

    private final JdbcTemplate jdbcTemplate;

    /** True if the lock was acquired. Must be called inside the same
     *  transaction as the calculation it protects. */
    public boolean tryAcquire() {
        Boolean acquired = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(?)", Boolean.class, SETTLEMENT_LOCK_KEY);
        return Boolean.TRUE.equals(acquired);
    }
}