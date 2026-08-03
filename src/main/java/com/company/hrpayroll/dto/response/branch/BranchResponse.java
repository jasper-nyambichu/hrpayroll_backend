// dto/response/branch/BranchResponse.java
package com.company.hrpayroll.dto.response.branch;

public record BranchResponse(
        Long id,
        String name,
        String code,
        String address,
        String phone,
        String email,
        Long managerId,
        String managerName,
        String status
) {}