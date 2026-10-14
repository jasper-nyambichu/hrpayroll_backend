// service/payroll/batch/PayrollRunService.java
package com.company.hrpayroll.service.payroll.batch;

import com.company.hrpayroll.dto.request.payroll.PayrollRunRequest;
import com.company.hrpayroll.dto.response.payroll.AgentPayrollSummaryResponse;
import com.company.hrpayroll.dto.response.payroll.PayrollRunResponse;
import com.company.hrpayroll.dto.response.payroll.PayslipResponse;

import java.util.List;

public interface PayrollRunService {
    PayrollRunResponse runPayroll(PayrollRunRequest request, Long initiatedByUserId);
    PayrollRunResponse getRunById(Long payrollRunId);
    List<PayslipResponse> getPayslipsForRun(Long payrollRunId);
    List<AgentPayrollSummaryResponse> getAgentSummariesForRun(Long payrollRunId);

    // One agent's summaries across every run, newest period first — backs the Agent Portal statements.
    List<AgentPayrollSummaryResponse> getAgentSummariesForAgent(Long agentId);
}