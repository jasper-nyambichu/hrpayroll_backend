// dto/request/auth/CreateUserRequest.java
package com.company.hrpayroll.dto.request.auth;

import com.company.hrpayroll.entity.auth.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Temporary password is required") String temporaryPassword,
        @NotNull(message = "Role is required") Role role,
        String displayName,
        Long employeeId,
        Long agentId,
        Long branchId
) {}