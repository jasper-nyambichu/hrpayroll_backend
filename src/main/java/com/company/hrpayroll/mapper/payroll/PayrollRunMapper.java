// mapper/payroll/PayrollRunMapper.java
package com.company.hrpayroll.mapper.payroll;

import com.company.hrpayroll.dto.response.payroll.PayrollRunResponse;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import org.springframework.stereotype.Service;

@Service
public class PayrollRunMapper {

    public PayrollRunResponse toPayrollRunResponse(PayrollRun run, int payslipCount, int agentCount) {
        return new PayrollRunResponse(
                run.getId(),
                run.getPeriodStart(),
                run.getPeriodEnd(),
                run.getStatus().name(),
                run.getTotalGrossPayroll(),
                run.getTotalNetPayroll(),
                payslipCount,
                agentCount,
                run.getScopeBranchIds(),
                run.getScopeAgentIds(),
                run.getScopeDepartmentIds()
        );
    }
}