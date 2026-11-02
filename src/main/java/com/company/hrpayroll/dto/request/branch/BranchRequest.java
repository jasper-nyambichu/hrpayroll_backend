// dto/request/branch/BranchRequest.java
package com.company.hrpayroll.dto.request.branch;

import jakarta.validation.constraints.NotBlank;

// The code is not here: the system gives every branch its next index code.
public record BranchRequest(
        @NotBlank(message = "Branch name is required") String name,
        String address,
        String phone,
        String email,
        Long managerId
) {}