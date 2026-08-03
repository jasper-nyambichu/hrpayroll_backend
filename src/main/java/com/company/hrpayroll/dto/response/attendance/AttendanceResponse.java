// dto/response/attendance/AttendanceResponse.java
package com.company.hrpayroll.dto.response.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceResponse(
        Long id,
        Long employeeId,
        String employeeName,
        Long branchId,
        LocalDate attendanceDate,
        LocalDateTime clockIn,
        LocalDateTime clockOut,
        String source,
        String status
) {}