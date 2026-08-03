// service/payroll/documents/AgentInvoiceGenerator.java
package com.company.hrpayroll.service.payroll.documents;

import com.company.hrpayroll.dto.response.payroll.GeneratedDocument;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.payroll.AgentPayrollSummary;
import com.company.hrpayroll.entity.payroll.PayrollRun;

public interface AgentInvoiceGenerator {
    GeneratedDocument generate(AgentPayrollSummary summary, Agent agent, PayrollRun run);
}