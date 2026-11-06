// dto/response/attendance/ManualCheckInResult.java
package com.company.hrpayroll.dto.response.attendance;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * What happened to one ticked employee. checkedIn is false with a message when they were refused
 * (already in, on leave, not active, another branch). clockInUtc lets the kiosk log the exact instant.
 */
public record ManualCheckInResult(
        Long employeeId,
        String employeeName,
        boolean checkedIn,
        Long attendanceId,
        LocalDateTime clockIn,
        Instant clockInUtc,
        String message
) {}
