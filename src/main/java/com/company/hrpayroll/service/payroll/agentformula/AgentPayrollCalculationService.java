// service/payroll/agentformula/AgentPayrollCalculationService.java
package com.company.hrpayroll.service.payroll.agentformula;

import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.payroll.AgentPayrollSummary;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.entity.payroll.Payslip;

import java.math.BigDecimal;
import java.util.List;

public interface AgentPayrollCalculationService {

    // Builds an AgentPayrollSummary in memory — does not persist it and does
    // not recover the advance itself. That happens in payroll/batch, once
    // this summary has been reviewed/saved, matching the same "calculate
    // first, mutate state deliberately after" pattern as the engine.
    AgentPayrollSummary calculateForAgent(
            Agent agent,
            List<Employee> agentEmployees,
            List<Payslip> agentPayslips,
            BigDecimal recoverableAdvanceBalance,
            PayrollRun run
    );
}