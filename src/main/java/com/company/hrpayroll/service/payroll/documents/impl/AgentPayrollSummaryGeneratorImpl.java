// service/payroll/documents/impl/AgentPayrollSummaryGeneratorImpl.java
package com.company.hrpayroll.service.payroll.documents.impl;

import com.company.hrpayroll.dto.response.payroll.GeneratedDocument;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.payroll.AgentPayrollSummary;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.service.payroll.documents.AgentPayrollSummaryGenerator;
import com.company.hrpayroll.service.payroll.documents.support.PdfDocumentWriter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;

import static com.company.hrpayroll.service.payroll.documents.support.AmountFormatter.format;

@Service
public class AgentPayrollSummaryGeneratorImpl implements AgentPayrollSummaryGenerator {

    @Override
    public GeneratedDocument generate(AgentPayrollSummary summary, Agent agent, PayrollRun run) {
        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {

            writer.title("Agent Payroll Summary");
            writer.subtitle("Agent: " + agent.getName());
            writer.subtitle("Payroll period: " + run.getPeriodStart() + " to " + run.getPeriodEnd());
            writer.subtitle("Employees: " + summary.getEmployeeCount());
            writer.divider();

            writer.sectionHeading("Additions");
            writer.row("Gross Salaries", format(summary.getGrossSalaries()));
            writer.row("Service Charge", format(summary.getServiceCharge()));
            writer.row("SHIF", format(summary.getShifTotal()));
            writer.row("NSSF", format(summary.getNssfTotal()));
            writer.row("Housing Levy", format(summary.getHousingLevyTotal()));
            writer.row("PAYE", format(summary.getPayeTotal()));
            writer.row("Employee Loan Deductions", format(summary.getEmployeeLoanDeductionsTotal()));

            writer.sectionHeading("Deductions");
            writer.row("Agent Advance Recovery", "(" + format(summary.getAdvanceRecoveryTotal()) + ")");

            writer.divider();
            writer.boldRow("Subtotal Before VAT", format(summary.getSubtotalBeforeVat()));

            writer.sectionHeading("VAT");
            if (summary.isVatApplied()) {
                writer.row("VAT Rate", format(summary.getVatRateUsed()) + "%");
                writer.row("VAT Amount", format(summary.getVatAmount()));
            } else {
                writer.row("VAT", "Not applicable — agent is not VAT-registered");
            }

            writer.divider();
            writer.boldRow("Final Amount Payable to Agent", format(summary.getFinalAmountPayable()));

            byte[] content = writer.build();
            String fileName = "agent-payroll-summary-" + agent.getId() + "-run-" + run.getId() + ".pdf";
            return new GeneratedDocument(fileName, "application/pdf", content);

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate agent payroll summary PDF", e);
        }
    }
}