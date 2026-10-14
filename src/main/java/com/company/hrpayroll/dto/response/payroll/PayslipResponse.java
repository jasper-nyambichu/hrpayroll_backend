// dto/response/payroll/PayslipResponse.java
package com.company.hrpayroll.dto.response.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;

// periodStart/periodEnd come from the PayrollRun, so a payslip can be shown
// (and listed) by period without a second call.
public record PayslipResponse(
        Long id,
        Long payrollRunId,
        LocalDate periodStart,
        LocalDate periodEnd,
        Long employeeId,
        String employeeName,
        String employeeNumber,
        BigDecimal basicSalary,
        BigDecimal houseAllowance,
        BigDecimal transportAllowance,
        BigDecimal overtimePay,
        BigDecimal bonus,
        BigDecimal otherAllowances,
        BigDecimal unpaidLeaveDeduction,
        BigDecimal grossPay,
        BigDecimal shifDeduction,
        BigDecimal nssfDeduction,
        BigDecimal housingLevyDeduction,
        BigDecimal payeDeduction,
        BigDecimal nitaDeduction,
        BigDecimal loanDeduction,
        BigDecimal otherDeductions,
        BigDecimal netPay
) {}