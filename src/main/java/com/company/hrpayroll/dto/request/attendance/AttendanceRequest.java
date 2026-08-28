// dto/request/attendance/AttendanceRequest.java
package com.company.hrpayroll.dto.request.attendance;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AttendanceRequest(
        @NotNull(message = "Employee is required") Long employeeId,
        @NotNull(message = "Clock-in time is required") LocalDateTime clockIn,
        LocalDateTime clockOut
) {}
