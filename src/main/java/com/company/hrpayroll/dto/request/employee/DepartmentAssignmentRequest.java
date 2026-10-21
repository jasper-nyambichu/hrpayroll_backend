// dto/request/employee/DepartmentAssignmentRequest.java
package com.company.hrpayroll.dto.request.employee;

import jakarta.validation.constraints.NotNull;

public record DepartmentAssignmentRequest(
        @NotNull(message = "Department is required") Long departmentId
) {}