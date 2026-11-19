package com.company.hrpayroll.dto.response.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One agent's payment for a period, line by line: the same shape for the Admin's preview (nothing saved yet,
 * {@code settlementId} null) and for a payment that was run, which the Admin and the agent both read. So what the
 * agent sees is exactly what the Admin ran.
 */
public record AgentPaymentDetail(
        Long settlementId,
        Long agentId,
        String agentName,
        LocalDate periodStart,
        LocalDate periodEnd,
        String payBasis,             // SALARY, or DAILY_RATE for runs made before salaries were used
        List<EmployeeLine> employees,
        BigDecimal salaries,         // the employees' pay for the period
        BigDecimal paye,
        BigDecimal nssf,
        BigDecimal nita,
        BigDecimal agentFee,
        BigDecimal vatRate,
        BigDecimal vat,
        BigDecimal loans,
        BigDecimal damages,
        List<AdvanceLine> advances,  // each advance taken back, with its dates
        BigDecimal advanceRecovered,
        BigDecimal amountPayable,
        Long paymentRequestId,
        List<String> warnings        // e.g. employees without a recorded salary (preview)
) {

    public record EmployeeLine(
            Long employeeId,
            String employeeNumber,
            String employeeName,
            String branchName,
            BigDecimal monthlySalary,
            int daysWorked,
            int offDaysPaid,
            BigDecimal dailyRate,
            BigDecimal pay,
            BigDecimal loan,
            BigDecimal damage,
            BigDecimal netPay
    ) {}

    public record AdvanceLine(
            Long advanceId,
            String purpose,
            BigDecimal advanceAmount,
            LocalDate requestedOn,
            LocalDate approvedOn,
            BigDecimal deducted,        // taken off this payment (shown with a minus)
            BigDecimal balanceAfter
    ) {}
}
