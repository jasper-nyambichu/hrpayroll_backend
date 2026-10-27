// dto/response/dayswork/DaysWorkedResponse.java
package com.company.hrpayroll.dto.response.dayswork;

import java.time.LocalDate;
import java.time.LocalDateTime;

// One employee's days in one period. While the period is open (locked = false) daysWorked simply
// equals systemDays, computed live from attendance; once locked it is the final figure.
public record DaysWorkedResponse(
        Long employeeId,
        String employeeName,
        String employeeNumber,
        Long branchId,
        LocalDate periodStart,
        LocalDate periodEnd,
        int systemDays,
        int daysWorked,
        boolean adjusted,
        String adjustmentReason,
        boolean locked,
        LocalDateTime lockedAt
) {}
