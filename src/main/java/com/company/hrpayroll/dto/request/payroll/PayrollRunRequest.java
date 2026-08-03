// dto/request/payroll/PayrollRunRequest.java
package com.company.hrpayroll.dto.request.payroll;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record PayrollRunRequest(
        @NotNull(message = "Period start is required") LocalDate periodStart,
        @NotNull(message = "Period end is required") LocalDate periodEnd,
        boolean allEmployees,
        List<Long> branchIds,
        List<Long> agentIds,
        List<Long> departmentIds
) {}