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
        boolean active,
        // The employee record of the person behind a Branch Manager or Back Office login, if linked.
        Long employeeId
) {}