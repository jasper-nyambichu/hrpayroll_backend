// dto/request/dayswork/DaysWorkedLockRequest.java
package com.company.hrpayroll.dto.request.dayswork;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

// Locks the days worked for every active employee of one branch in one pay period. Employees not
// listed in adjustments are locked at the figure the attendance records give.
public record DaysWorkedLockRequest(
        @NotNull(message = "Branch is required") Long branchId,
        @NotNull(message = "Period start is required") LocalDate periodStart,
        @NotNull(message = "Period end is required") LocalDate periodEnd,
        @Valid List<DaysWorkedAdjustmentRequest> adjustments
) {}
