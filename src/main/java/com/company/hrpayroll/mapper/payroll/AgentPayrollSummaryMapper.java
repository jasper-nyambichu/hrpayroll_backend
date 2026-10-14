// mapper/payroll/AgentPayrollSummaryMapper.java
package com.company.hrpayroll.mapper.payroll;

import com.company.hrpayroll.dto.response.payroll.AgentPayrollSummaryResponse;
import com.company.hrpayroll.entity.payroll.AgentPayrollSummary;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import org.springframework.stereotype.Service;

@Service
public class AgentPayrollSummaryMapper {

    public AgentPayrollSummaryResponse toAgentPayrollSummaryResponse(
            AgentPayrollSummary summary, String agentName, PayrollRun run) {
        return new AgentPayrollSummaryResponse(
                summary.getId(),
                summary.getPayrollRunId(),
                run.getPeriodStart(),
                run.getPeriodEnd(),
                summary.getAgentId(),
                agentName,
                summary.getEmployeeCount(),
                summary.getGrossSalaries(),
                summary.getServiceCharge(),
                summary.getShifTotal(),
                summary.getNssfTotal(),
                summary.getHousingLevyTotal(),
                summary.getPayeTotal(),
                summary.getEmployeeLoanDeductionsTotal(),
                summary.getAdvanceRecoveryTotal(),
                summary.getSubtotalBeforeVat(),
                summary.isVatApplied(),
                summary.getVatRateUsed(),
                summary.getVatAmount(),
                summary.getFinalAmountPayable()
        );
    }
}