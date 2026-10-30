// controller/finance/FinanceController.java
package com.company.hrpayroll.controller.finance;

import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.payroll.AgentPayrollSummaryResponse;
import com.company.hrpayroll.dto.response.payroll.FinanceRunSummaryResponse;
import com.company.hrpayroll.service.payroll.finance.PayrollFinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Read-only money views for the Accounts Office: what each payroll run costs in cash, and every
// agent's payment statement. Nothing here changes data.
@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final PayrollFinanceService payrollFinanceService;

    private static final String FINANCE_READ_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER', 'FINANCE_OFFICER', 'AUDITOR')";

    // Every run's cash requirement, newest period first.
    @GetMapping("/runs")
    @PreAuthorize(FINANCE_READ_ROLES)
    public ApiResponse<List<FinanceRunSummaryResponse>> getRunSummaries() {
        return ApiResponse.success(payrollFinanceService.getRunSummaries());
    }

    @GetMapping("/runs/{runId}")
    @PreAuthorize(FINANCE_READ_ROLES)
    public ApiResponse<FinanceRunSummaryResponse> getRunSummary(@PathVariable Long runId) {
        return ApiResponse.success(payrollFinanceService.getRunSummary(runId));
    }

    // Every agent statement across every run, newest period first — backs the invoice list.
    @GetMapping("/agent-summaries")
    @PreAuthorize(FINANCE_READ_ROLES)
    public ApiResponse<List<AgentPayrollSummaryResponse>> getAllAgentSummaries() {
        return ApiResponse.success(payrollFinanceService.getAllAgentSummaries());
    }
}