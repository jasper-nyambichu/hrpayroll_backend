// dto/request/dayswork/DaysWorkedAdjustmentRequest.java
package com.company.hrpayroll.dto.request.dayswork;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// One employee whose days the officer corrects before locking (a missed scan, a forgotten
// clock-out). The reason is mandatory and stays on the locked record.
public record DaysWorkedAdjustmentRequest(
        @NotNull(message = "Employee is required") Long employeeId,
        @NotNull(message = "Days worked is required")
        @Min(value = 0, message = "Days worked cannot be negative") Integer daysWorked,
        @NotBlank(message = "A reason is required for an adjustment")
        @Size(max = 500, message = "Reason must be at most 500 characters") String reason
) {}
