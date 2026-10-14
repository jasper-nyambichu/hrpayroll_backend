// dto/response/payroll/AgentPayrollSummaryResponse.java
package com.company.hrpayroll.dto.response.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;

// Mirrors the formula from the proposal exactly — this is what the
// invoice, voucher, and summary documents are generated from.
// periodStart/periodEnd come from the PayrollRun so the Agent Portal can
// list statements by period.
public record AgentPayrollSummaryResponse(
        Long id,
        Long payrollRunId,
        LocalDate periodStart,
        LocalDate periodEnd,
        Long agentId,
        String agentName,
        Integer employeeCount,
        BigDecimal grossSalaries,
        BigDecimal serviceCharge,
        BigDecimal shifTotal,
        BigDecimal nssfTotal,
        BigDecimal housingLevyTotal,
        BigDecimal payeTotal,
        BigDecimal employeeLoanDeductionsTotal,
        BigDecimal advanceRecoveryTotal,
        BigDecimal subtotalBeforeVat,
        boolean vatApplied,
        BigDecimal vatRateUsed,
        BigDecimal vatAmount,
        BigDecimal finalAmountPayable
) {}