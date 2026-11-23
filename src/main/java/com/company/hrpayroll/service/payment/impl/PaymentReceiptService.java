// service/payment/impl/PaymentReceiptService.java
package com.company.hrpayroll.service.payment.impl;

import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.settlement.AgentPaymentDetail;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.agentadvance.AgentAdvance;
import com.company.hrpayroll.entity.payment.PaymentRequest;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.agentadvance.AgentAdvanceRepository;
import com.company.hrpayroll.repository.payment.PaymentEvidenceRepository;
import com.company.hrpayroll.repository.payment.PaymentRequestRepository;
import com.company.hrpayroll.repository.settlement.AgentSettlementRepository;
import com.company.hrpayroll.service.common.pdf.DateLabel;
import com.company.hrpayroll.service.common.pdf.PdfDocumentWriter;
import com.company.hrpayroll.service.company.CompanyService;
import com.company.hrpayroll.service.notification.NotificationRecipients;
import com.company.hrpayroll.service.settlement.AgentSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.company.hrpayroll.service.common.pdf.AmountFormatter.format;

/**
 * The receipt of one agent payment, to track how the transaction happened: who was paid, how (M-Pesa code, cheque
 * number, bank and date, or cash receipt), when and by whom; and for an invoice, which branches' employees the money
 * was for and how much each receives. For an advance, the advance it pays. Downloaded by the Accounts Office and
 * the Admin (and the agent paid).
 */
@Service
@RequiredArgsConstructor
public class PaymentReceiptService {

    private static final float[] TWO_COLUMNS = {170f, 325f};
    private static final float[] EMPLOYEE_COLUMNS = {80f, 230f, 185f};
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private final PaymentRequestRepository paymentRequestRepository;
    private final PaymentEvidenceRepository evidenceRepository;
    private final AgentRepository agentRepository;
    private final AgentAdvanceRepository advanceRepository;
    private final AgentSettlementRepository settlementRepository;
    private final AgentSettlementService settlementService;
    private final CompanyService companyService;
    private final NotificationRecipients recipients;

    /** agentId: when an agent asks, only their own payment's receipt. */
    @Transactional(readOnly = true)
    public GeneratedDocument receipt(Long paymentRequestId, Long agentId) {
        PaymentRequest request = paymentRequestRepository.findById(paymentRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentRequestId));
        if (agentId != null && !agentId.equals(request.getAgentId())) {
            throw new ResourceNotFoundException("Payment not found with id: " + paymentRequestId);
        }
        if (!"RELEASED".equals(request.getStatus())) {
            throw new InvalidOperationException("A receipt is available once the payment has been made");
        }
        Agent agent = agentRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + request.getAgentId()));
        AgentPaymentDetail invoice = settlementRepository.findFirstByPaymentRequestId(request.getId())
                .map(settlement -> settlementService.getDetail(settlement.getId()))
                .orElse(null);
        AgentAdvance advance = advanceRepository.findFirstByPaymentRequestId(request.getId()).orElse(null);

        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            String paidOn = request.getPaidOn() != null ? request.getPaidOn().format(DAY)
                    : request.getReleasedAt() != null ? request.getReleasedAt().format(DAY) : "-";
            writer.documentHeader(
                    "Payment Receipt",
                    List.of(
                            new PdfDocumentWriter.Field("Receipt", "#" + request.getId()),
                            new PdfDocumentWriter.Field("Paid on", paidOn)),
                    companyService.getLetterhead(),
                    List.of(
                            new PdfDocumentWriter.Field("Paid to", agent.getName()),
                            new PdfDocumentWriter.Field("KRA PIN", agent.getKraPin() != null ? agent.getKraPin() : "-"),
                            new PdfDocumentWriter.Field("For", invoice != null
                                    ? "Invoice #" + invoice.settlementId() + ", " + DateLabel.range(invoice.periodStart(), invoice.periodEnd())
                                    : advance != null ? "Advance #" + advance.getId() : request.getPurpose())));

            // How it was paid.
            writer.sectionHeading("How it was paid");
            for (String[] row : howPaid(request)) {
                writer.tableRow(TWO_COLUMNS, List.of(row[0], row[1]), false);
            }

            // Who the money was for.
            if (invoice != null) {
                Map<String, List<AgentPaymentDetail.EmployeeLine>> byBranch = new LinkedHashMap<>();
                for (AgentPaymentDetail.EmployeeLine line : invoice.employees()) {
                    String branch = line.branchName() != null ? line.branchName() : "No branch";
                    byBranch.computeIfAbsent(branch, k -> new ArrayList<>()).add(line);
                }
                writer.sectionHeading("Employees paid, by branch");
                for (Map.Entry<String, List<AgentPaymentDetail.EmployeeLine>> branch : byBranch.entrySet()) {
                    writer.tableRow(EMPLOYEE_COLUMNS, List.of("Branch", branch.getKey(), branch.getValue().size() + " employee(s)"), true);
                    BigDecimal total = BigDecimal.ZERO;
                    for (AgentPaymentDetail.EmployeeLine line : branch.getValue()) {
                        writer.tableRow(EMPLOYEE_COLUMNS, List.of(
                                line.employeeNumber() != null ? line.employeeNumber() : "-",
                                line.employeeName(),
                                "KES " + format(line.netPay())), false);
                        total = total.add(line.netPay());
                    }
                    writer.tableRow(EMPLOYEE_COLUMNS, List.of("", branch.getKey() + " total", "KES " + format(total)), true);
                }
                writer.note("Each employee's amount is their net pay on the invoice. The amount paid also covers PAYE, NSSF, "
                        + "NITA, the agent fee and VAT, less any advance taken back, exactly as on invoice #" + invoice.settlementId() + ".");
            } else if (advance != null) {
                writer.sectionHeading("Advance");
                writer.tableRow(TWO_COLUMNS, List.of("Purpose", advance.getPurpose() != null ? advance.getPurpose() : "-"), false);
                writer.tableRow(TWO_COLUMNS, List.of("Asked on", advance.getCreatedAt() != null ? advance.getCreatedAt().format(DAY) : "-"), false);
                writer.tableRow(TWO_COLUMNS, List.of("Approved on", advance.getDecidedAt() != null ? advance.getDecidedAt().format(DAY) : "-"), false);
                writer.tableRow(TWO_COLUMNS, List.of("Advance amount", "KES " + format(advance.getAmount())), false);
                writer.note("The advance is taken back from the agent's next payments.");
            } else {
                writer.sectionHeading("Payment");
                writer.tableRow(TWO_COLUMNS, List.of("Purpose", request.getPurpose()), false);
            }

            writer.netBar("Amount Paid", "KES " + format(request.getAmount()));
            writer.signatureBlock("Payment Details:", List.of(summary(request)),
                    recipients.nameOf(request.getReleasedByUserId()) != null ? recipients.nameOf(request.getReleasedByUserId()) : "Accounts Office",
                    agent.getName());

            String fileName = "payment-receipt-" + request.getId() + ".pdf";
            return new GeneratedDocument(fileName, "application/pdf", writer.build());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate the payment receipt PDF", e);
        }
    }

    private List<String[]> howPaid(PaymentRequest request) {
        List<String[]> rows = new ArrayList<>();
        String method = request.getPaymentMethod();
        rows.add(new String[]{"Mode of payment", method == null ? "Released" : switch (method) {
            case "MPESA" -> "M-Pesa";
            case "CHEQUE" -> "Cheque";
            default -> "Cash";
        }});
        if ("MPESA".equals(method)) rows.add(new String[]{"M-Pesa code", request.getReleaseReference()});
        if ("CHEQUE".equals(method)) {
            rows.add(new String[]{"Cheque number", request.getChequeNumber()});
            rows.add(new String[]{"Bank", request.getChequeBank()});
            rows.add(new String[]{"Date on cheque", request.getChequeDate() != null ? request.getChequeDate().format(DAY) : "-"});
        }
        if (method == null || "MANUAL".equals(method)) {
            rows.add(new String[]{"MANUAL".equals(method) ? "Receipt / reference" : "Reference",
                    request.getReleaseReference() != null ? request.getReleaseReference() : "-"});
        }
        rows.add(new String[]{"Amount", "KES " + format(request.getAmount())});
        rows.add(new String[]{"Recorded by", orDash(recipients.nameOf(request.getReleasedByUserId()))});
        rows.add(new String[]{"Recorded at", request.getReleasedAt() != null ? request.getReleasedAt().format(WHEN) : "-"});
        if (request.getPaymentNote() != null) rows.add(new String[]{"Note", request.getPaymentNote()});
        rows.add(new String[]{"Proof of payment", evidenceRepository.existsByPaymentRequestId(request.getId()) ? "Attached in the system" : "None attached"});
        return rows;
    }

    private static String summary(PaymentRequest request) {
        String method = request.getPaymentMethod();
        if ("MPESA".equals(method)) return "M-Pesa " + request.getReleaseReference();
        if ("CHEQUE".equals(method)) return "Cheque " + request.getChequeNumber() + ", " + request.getChequeBank();
        if ("MANUAL".equals(method)) return "Cash, receipt " + request.getReleaseReference();
        return request.getReleaseReference() != null ? request.getReleaseReference() : "Released";
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
