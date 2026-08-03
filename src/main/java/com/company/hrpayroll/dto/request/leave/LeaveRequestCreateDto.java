// dto/request/leave/LeaveRequestCreateDto.java
package com.company.hrpayroll.dto.request.leave;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LeaveRequestCreateDto(
        @NotNull(message = "Leave type is required") Long leaveTypeId,
        @NotNull(message = "Start date is required") LocalDate startDate,
        @NotNull(message = "End date is required") LocalDate endDate,
        String reason
) {}