// service/payroll/documents/impl/PayrollDocumentServiceImpl.java
package com.company.hrpayroll.service.payroll.documents.impl;

import com.company.hrpayroll.dto.response.payroll.GeneratedDocument;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.payroll.AgentPayrollSummary;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.payroll.AgentPayrollSummaryRepository;
import com.company.hrpayroll.repository.payroll.PayrollRunRepository;
import com.company.hrpayroll.service.payroll.documents.AgentInvoiceGenerator;
import com.company.hrpayroll.service.payroll.documents.AgentPayrollSummaryGenerator;
import com.company.hrpayroll.service.payroll.documents.PayrollDocumentService;
import com.company.hrpayroll.service.payroll.documents.PaymentVoucherGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayrollDocumentServiceImpl implements PayrollDocumentService {

    private final AgentPayrollSummaryRepository agentPayrollSummaryRepository;
    private final AgentRepository agentRepository;
    private final PayrollRunRepository payrollRunRepository;

    private final AgentInvoiceGenerator agentInvoiceGenerator;
    private final PaymentVoucherGenerator paymentVoucherGenerator;
    private final AgentPayrollSummaryGenerator agentPayrollSummaryGenerator;

    @Override
    public GeneratedDocument getAgentInvoice(Long payrollRunId, Long agentId) {
        DocumentContext ctx = resolveContext(payrollRunId, agentId);
        return agentInvoiceGenerator.generate(ctx.summary(), ctx.agent(), ctx.run());
    }

    @Override
    public GeneratedDocument getPaymentVoucher(Long payrollRunId, Long agentId) {
        DocumentContext ctx = resolveContext(payrollRunId, agentId);
        return paymentVoucherGenerator.generate(ctx.summary(), ctx.agent(), ctx.run());
    }

    @Override
    public GeneratedDocument getAgentPayrollSummaryDocument(Long payrollRunId, Long agentId) {
        DocumentContext ctx = resolveContext(payrollRunId, agentId);
        return agentPayrollSummaryGenerator.generate(ctx.summary(), ctx.agent(), ctx.run());
    }

    private DocumentContext resolveContext(Long payrollRunId, Long agentId) {
        PayrollRun run = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found with id: " + payrollRunId));

        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + agentId));

        AgentPayrollSummary summary = agentPayrollSummaryRepository
                .findByPayrollRunIdAndAgentId(payrollRunId, agentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payroll summary found for agent " + agentId + " in run " + payrollRunId));

        return new DocumentContext(run, agent, summary);
    }

    private record DocumentContext(PayrollRun run, Agent agent, AgentPayrollSummary summary) {}
}