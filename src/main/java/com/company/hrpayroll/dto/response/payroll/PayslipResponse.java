// dto/response/payroll/PayslipResponse.java
package com.company.hrpayroll.dto.response.payroll;

import java.math.BigDecimal;

public record PayslipResponse(
        Long id,
        Long payrollRunId,
        Long employeeId,
        String employeeName,
        BigDecimal basicSalary,
        BigDecimal houseAllowance,
        BigDecimal transportAllowance,
        BigDecimal overtimePay,
        BigDecimal bonus,
        BigDecimal otherAllowances,
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