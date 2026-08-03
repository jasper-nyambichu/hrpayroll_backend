// service/payroll/documents/impl/AgentInvoiceGeneratorImpl.java
package com.company.hrpayroll.service.payroll.documents.impl;

import com.company.hrpayroll.dto.response.payroll.GeneratedDocument;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.payroll.AgentPayrollSummary;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.service.payroll.documents.AgentInvoiceGenerator;
import com.company.hrpayroll.service.payroll.documents.support.PdfDocumentWriter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;

import static com.company.hrpayroll.service.payroll.documents.support.AmountFormatter.format;

@Service
public class AgentInvoiceGeneratorImpl implements AgentInvoiceGenerator {

    @Override
    public GeneratedDocument generate(AgentPayrollSummary summary, Agent agent, PayrollRun run) {
        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {

            writer.title("Agent Invoice");
            writer.subtitle("Invoice date: " + LocalDate.now());
            writer.subtitle("Agent KRA PIN: " + (agent.getKraPin() != null ? agent.getKraPin() : "-"));
            writer.divider();

            writer.row("Billed to", "Company Payroll Department");
            writer.row("Supplier", agent.getName());
            writer.row("Payroll Period", run.getPeriodStart() + " to " + run.getPeriodEnd());
            writer.divider();

            writer.row("Amount Before VAT", format(summary.getSubtotalBeforeVat()));

            if (summary.isVatApplied()) {
                writer.row("VAT (" + format(summary.getVatRateUsed()) + "%)", format(summary.getVatAmount()));
            } else {
                writer.row("VAT", "Not applicable — not VAT-registered");
            }

            writer.divider();
            writer.boldRow("Total Amount Due", format(summary.getFinalAmountPayable()));

            byte[] content = writer.build();
            String fileName = "agent-invoice-" + agent.getId() + "-run-" + run.getId() + ".pdf";
            return new GeneratedDocument(fileName, "application/pdf", content);

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate agent invoice PDF", e);
        }
    }
}