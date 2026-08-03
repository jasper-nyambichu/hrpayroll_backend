// dto/response/leave/LeaveRequestResponse.java
package com.company.hrpayroll.dto.response.leave;

import java.time.LocalDate;

public record LeaveRequestResponse(
        Long id,
        Long employeeId,
        String employeeName,
        Long leaveTypeId,
        String leaveTypeName,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        String status,
        String rejectionReason
) {}