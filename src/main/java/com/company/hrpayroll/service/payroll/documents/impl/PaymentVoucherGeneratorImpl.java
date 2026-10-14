// service/payroll/documents/impl/PaymentVoucherGeneratorImpl.java
package com.company.hrpayroll.service.payroll.documents.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.response.payroll.GeneratedDocument;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.payroll.AgentPayrollSummary;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.service.payroll.documents.PaymentVoucherGenerator;
import com.company.hrpayroll.service.payroll.documents.support.PdfDocumentWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;

import static com.company.hrpayroll.service.payroll.documents.support.AmountFormatter.format;

@Service
@RequiredArgsConstructor
public class PaymentVoucherGeneratorImpl implements PaymentVoucherGenerator {

    private final BusinessClock businessClock;

    @Override
    public GeneratedDocument generate(AgentPayrollSummary summary, Agent agent, PayrollRun run) {
        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {

            writer.title("Payment Voucher");
            writer.subtitle("Voucher date: " + businessClock.today());
            writer.divider();

            writer.row("Payee", agent.getName());
            writer.row("Bank", agent.getBankName() != null ? agent.getBankName() : "-");
            writer.row("Account Number", agent.getBankAccountNumber() != null ? agent.getBankAccountNumber() : "-");
            writer.row("Payroll Period", run.getPeriodStart() + " to " + run.getPeriodEnd());
            writer.divider();

            writer.boldRow("Amount Payable", format(summary.getFinalAmountPayable()));
            writer.spacer(30f);

            writer.row("Prepared by", "________________________");
            writer.row("Approved by", "________________________");
            writer.row("Date", "________________________");

            byte[] content = writer.build();
            String fileName = "payment-voucher-" + agent.getId() + "-run-" + run.getId() + ".pdf";
            return new GeneratedDocument(fileName, "application/pdf", content);

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate payment voucher PDF", e);
        }
    }
}