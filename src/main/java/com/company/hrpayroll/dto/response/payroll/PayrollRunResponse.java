// dto/response/payroll/PayrollRunResponse.java
package com.company.hrpayroll.dto.response.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayrollRunResponse(
        Long id,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,
        BigDecimal totalGrossPayroll,
        BigDecimal totalNetPayroll,
        int payslipCount,
        int agentCount
) {}