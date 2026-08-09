// dto/response/auth/AuthResponse.java
package com.company.hrpayroll.dto.response.auth;

/**
 * Issued by every login surface (staff, employee, agent).
 *
 * Carries the caller's identity alongside the tokens because the Next.js
 * frontend wraps this into its own httpOnly session cookie and needs the
 * scoping keys — employeeId / agentId / branchId — to resolve "my" routes
 * (/employees/me, /agents/me, /branches/my-branch) without a second lookup.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String username,
        String role,
        String displayName,
        Long employeeId,
        Long agentId,
        Long branchId,
        boolean mustChangePassword
) {}
