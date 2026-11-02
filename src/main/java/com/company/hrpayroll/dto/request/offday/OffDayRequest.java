// dto/request/offday/OffDayRequest.java
package com.company.hrpayroll.dto.request.offday;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record OffDayRequest(
        @NotNull(message = "Employee is required") Long employeeId,
        @NotNull(message = "Start date is required") LocalDate startDate,
        @NotNull(message = "End date is required") LocalDate endDate,
        @Size(max = 255, message = "Reason cannot exceed 255 characters") String reason
) {}
