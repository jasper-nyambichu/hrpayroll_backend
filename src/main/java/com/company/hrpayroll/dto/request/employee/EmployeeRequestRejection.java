// dto/request/employee/EmployeeRequestRejection.java
package com.company.hrpayroll.dto.request.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeRequestRejection(
        @NotBlank(message = "A rejection reason is required")
        @Size(max = 500, message = "Reason must be 500 characters or fewer") String reason
) {}