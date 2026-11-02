// dto/response/offday/OffDayResponse.java
package com.company.hrpayroll.dto.response.offday;

import java.time.LocalDate;

public record OffDayResponse(
        Long id,
        Long employeeId,
        String employeeNumber,
        String employeeName,
        Long branchId,
        String branchName,
        Long agentId,
        String agentName,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {}
