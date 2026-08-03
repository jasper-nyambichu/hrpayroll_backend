// dto/request/shift/ShiftAssignmentRequest.java
package com.company.hrpayroll.dto.request.shift;

import jakarta.validation.constraints.NotNull;

public record ShiftAssignmentRequest(
        @NotNull(message = "Shift is required") Long shiftId,
        Long employeeId,
        Long departmentId,
        Long branchId
) {}