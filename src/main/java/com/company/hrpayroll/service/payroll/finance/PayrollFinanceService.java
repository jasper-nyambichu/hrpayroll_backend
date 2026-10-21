// service/payroll/finance/PayrollFinanceService.java
package com.company.hrpayroll.service.payroll.finance;

import com.company.hrpayroll.dto.response.payroll.AgentPayrollSummaryResponse;
import com.company.hrpayroll.dto.response.payroll.FinanceRunSummaryResponse;

import java.util.List;

// Read-only financial views over payroll runs, for Finance Officer / Auditor.
public interface PayrollFinanceService {

    // Every run's cash requirement, newest period first.
    List<FinanceRunSummaryResponse> getRunSummaries();

    FinanceRunSummaryResponse getRunSummary(Long runId);

    // Every agent payment statement across every run, newest period first — backs the invoice list.
    List<AgentPayrollSummaryResponse> getAllAgentSummaries();
}