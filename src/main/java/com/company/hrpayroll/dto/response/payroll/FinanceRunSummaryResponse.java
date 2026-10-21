// dto/response/payroll/FinanceRunSummaryResponse.java
package com.company.hrpayroll.dto.response.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;

// What one payroll run costs the company in cash, split the way the money actually moves:
//  - direct employees: the company pays their net salary and remits their statutory amounts;
//  - agent-supplied staff: the company pays ONE amount to the agent (which already includes
//    their statutory amounts — the agent remits those), so their payslips are NOT paid twice.
public record FinanceRunSummaryResponse(
        Long runId,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,

        long directEmployees,
        BigDecimal directNetPay,
        BigDecimal directPaye,
        BigDecimal directNssf,
        BigDecimal directShif,
        BigDecimal directHousingLevy,
        BigDecimal directNita,           // employer levy on direct staff
        BigDecimal directLoanRecoveries, // withheld from direct staff pay for loans

        long agentCount,
        BigDecimal agentPayable,         // total of the agents' Final Amount Payable
        BigDecimal agentVat,
        BigDecimal agentPaye,            // statutory amounts inside the agent payments
        BigDecimal agentNssf,
        BigDecimal agentShif,
        BigDecimal agentHousingLevy,

        BigDecimal totalCashRequirement  // direct net + direct statutory + direct NITA + agent payable
) {}