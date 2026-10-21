// dto/request/department/DepartmentRequest.java
package com.company.hrpayroll.dto.request.department;

import jakarta.validation.constraints.NotBlank;

// branchId is accepted but ignored: departments are company-wide.
public record DepartmentRequest(
        @NotBlank(message = "Department name is required") String name,
        Long branchId,
        Long headOfDepartmentId
) {}