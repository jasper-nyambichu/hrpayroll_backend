// dto/request/department/DepartmentRequest.java
package com.company.hrpayroll.dto.request.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DepartmentRequest(
        @NotBlank(message = "Department name is required") String name,
        @NotNull(message = "Branch is required") Long branchId,
        Long headOfDepartmentId
) {}