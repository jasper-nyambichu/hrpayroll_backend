// dto/response/auth/UserSummaryResponse.java
package com.company.hrpayroll.dto.response.auth;

public record UserSummaryResponse(
        Long id,
        String username,
        String role,
        String displayName,
        Long branchId,
        Long agentId,
        boolean active,
        boolean canRunPayments
) {}