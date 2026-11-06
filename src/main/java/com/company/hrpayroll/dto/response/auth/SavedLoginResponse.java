package com.company.hrpayroll.dto.response.auth;

import java.time.LocalDateTime;

/**
 * A login the Admin set, to share it again. {@code password} is null once the person changed or reset it
 * themselves; {@code changedByOwnerAt} then says when.
 */
public record SavedLoginResponse(
        Long id,
        String username,
        String email,
        String password,
        LocalDateTime passwordSetAt,
        LocalDateTime changedByOwnerAt
) {}
