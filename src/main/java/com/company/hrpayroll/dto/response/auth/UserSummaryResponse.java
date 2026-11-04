// dto/response/auth/UserSummaryResponse.java
package com.company.hrpayroll.dto.response.auth;

public record UserSummaryResponse(
        Long id,
        String username,
        String role,
        String displayName,
        String email,
        Long branchId,
        Long agentId,
        boolean active
) {}