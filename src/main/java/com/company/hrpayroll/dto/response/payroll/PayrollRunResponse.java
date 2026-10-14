// dto/response/payroll/PayrollRunResponse.java
package com.company.hrpayroll.dto.response.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;

// scope* are comma-separated ids recorded when the run started; all three null
// means the run covered every active employee.
public record PayrollRunResponse(
        Long id,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,
        BigDecimal totalGrossPayroll,
        BigDecimal totalNetPayroll,
        int payslipCount,
        int agentCount,
        String scopeBranchIds,
        String scopeAgentIds,
        String scopeDepartmentIds
) {}