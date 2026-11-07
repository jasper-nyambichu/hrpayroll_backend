// service/settlement/impl/PayslipServiceImpl.java
package com.company.hrpayroll.service.settlement.impl;

import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.settlement.PayslipResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.damage.Damage;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.loan.Loan;
import com.company.hrpayroll.entity.loan.LoanRepayment;
import com.company.hrpayroll.entity.payment.EmployeePayment;
import com.company.hrpayroll.entity.settlement.AgentSettlement;
import com.company.hrpayroll.entity.settlement.EmployeePayslip;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.damage.DamageRepository;
import com.company.hrpayroll.repository.loan.LoanRepaymentRepository;
import com.company.hrpayroll.repository.loan.LoanRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.payment.EmployeePaymentRepository;
import com.company.hrpayroll.repository.settlement.AgentSettlementRepository;
import com.company.hrpayroll.repository.settlement.EmployeePayslipRepository;
import com.company.hrpayroll.service.common.pdf.DateLabel;
import com.company.hrpayroll.service.common.pdf.PdfDocumentWriter;
import com.company.hrpayroll.service.company.CompanyService;
import com.company.hrpayroll.service.settlement.PayslipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.company.hrpayroll.service.common.pdf.AmountFormatter.format;

@Service
@RequiredArgsConstructor
public class PayslipServiceImpl implements PayslipService {

    private static final float[] PAYMENT_COLUMNS = {165f, 165f, 165f};

    private final EmployeePayslipRepository payslipRepository;
    private final AgentSettlementRepository settlementRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeePaymentRepository paymentRepository;
    private final BranchRepository branchRepository;
    private final AgentRepository agentRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final LoanRepository loanRepository;
    private final DamageRepository damageRepository;
    private final CompanyService companyService;

    @Override
    public List<PayslipResponse> getAll() {
        return toResponses(payslipRepository.findAll());
    }

    @Override
    public List<PayslipResponse> getForBranch(Long branchId) {
        if (branchId == null) {
            return List.of();
        }
        List<Long> employeeIds = employeeRepository.findByBranchId(branchId).stream().map(Employee::getId).toList();
        return toResponses(payslipRepository.findByEmployeeIdIn(employeeIds));
    }

    // Goes by who the agent paid, not who the employee works for today: an employee who has since
    // moved agent stays on the statements of the agent who was given the money for them.
    @Override
    public List<PayslipResponse> getForAgent(Long agentId) {
        if (agentId == null) {
            return List.of();
        }
        List<Long> settlementIds = settlementRepository.findByAgentIdOrderByPeriodEndDescIdDesc(agentId).stream()
                .map(AgentSettlement::getId)
                .toList();
        return toResponses(payslipRepository.findBySettlementIdIn(settlementIds));
    }

    @Override
    public PayslipResponse getById(Long payslipId) {
        return toResponses(List.of(findOrThrow(payslipId))).get(0);
    }

    @Override
    public GeneratedDocument getPdf(Long payslipId) {
        PayslipResponse payslip = getById(payslipId);

        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            writePayslip(writer, payslip, companyService.getLetterhead());

            String fileName = "payslip-" + payslip.employeeNumber() + "-" + payslip.periodEnd() + ".pdf";
            return new GeneratedDocument(fileName, "application/pdf", writer.build());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate payslip PDF", e);
        }
    }

    static void writePayslip(PdfDocumentWriter writer, PayslipResponse payslip, PdfDocumentWriter.Letterhead company) throws IOException {
        writer.documentHeader(
                "Payslip",
                List.of(new PdfDocumentWriter.Field("Pay Period", DateLabel.range(payslip.periodStart(), payslip.periodEnd()))),
                company,
                List.of(
                        new PdfDocumentWriter.Field("Employee Name", orDash(payslip.employeeName())),
                        new PdfDocumentWriter.Field("Employee No", orDash(payslip.employeeNumber())),
                        new PdfDocumentWriter.Field("Branch", orDash(payslip.branchName())),
                        new PdfDocumentWriter.Field("Agent", orDash(payslip.agentName())),
                        new PdfDocumentWriter.Field("Days Worked", String.valueOf(payslip.daysWorked()))));

        writer.sectionTitle("Income Details");
        writer.moneyHeader("Amount (KES)");
        writer.moneyRow(1, payslip.daysWorked() + " days x " + format(payslip.dailyRate()), format(payslip.pay()));
        writer.moneyRow(2, "PAYE", format(payslip.paye()));
        writer.moneyRow(3, "NSSF", format(payslip.nssf()));
        writer.moneyRow(4, "NITA", format(payslip.nita()));
        writer.moneyTotal("Total Income", "KES " + format(payslip.pay().add(payslip.paye()).add(payslip.nssf()).add(payslip.nita())));

        // Each loan instalment and damage on its own line, so the employee can see what was taken and why.
        writer.sectionTitle("Deduction Details");
        int row = 1;
        if (payslip.loanItems().isEmpty()) {
            writer.moneyRow(row++, "Loans", format(payslip.loan()));
        } else {
            for (PayslipResponse.DeductionItem item : payslip.loanItems()) {
                writer.moneyRow(row++, item.label(), format(item.amount()));
            }
        }
        if (payslip.damageItems().isEmpty()) {
            writer.moneyRow(row, "Damages", format(payslip.damage()));
        } else {
            for (PayslipResponse.DeductionItem item : payslip.damageItems()) {
                String when = item.date() != null ? " (" + DateLabel.format(item.date()) + ")" : "";
                writer.moneyRow(row++, item.label() + when, format(item.amount()));
            }
        }
        writer.moneyTotal("Total Deductions", "KES " + format(payslip.loan().add(payslip.damage())));

        writer.netBar("Amount Paid to the Agent", "KES " + format(payslip.amountDue()));
        writer.note("Includes PAYE, NSSF and NITA, which the agent deducts and pays to the government.");

        if (!payslip.payments().isEmpty()) {
            writer.sectionHeading("Payments Recorded by the Agent");
            writer.tableRow(PAYMENT_COLUMNS, List.of("Date", "M-Pesa Code", "Amount (KES)"), true);
            for (PayslipResponse.RecordedPayment payment : payslip.payments()) {
                writer.tableRow(PAYMENT_COLUMNS, List.of(DateLabel.format(payment.paidOn()), payment.mpesaCode(), format(payment.amount())), false);
            }
        }

        writer.signatureBlock(
                "Payment Details:",
                List.of(
                        "Method: M-Pesa",
                        "Recorded: KES " + format(payslip.amountRecorded()),
                        "Not yet recorded: KES " + format(payslip.amountNotRecorded())),
                "Administrator",
                orDash(payslip.employeeName()));
    }

    // ---- helpers ----

    // Newest period first, then by name. Everything it needs is read once, not per payslip.
    private List<PayslipResponse> toResponses(List<EmployeePayslip> payslips) {
        if (payslips.isEmpty()) {
            return List.of();
        }

        Map<Long, AgentSettlement> settlements = byId(
                settlementRepository.findAllById(payslips.stream().map(EmployeePayslip::getSettlementId).distinct().toList()),
                AgentSettlement::getId);
        Map<Long, Employee> employees = byId(
                employeeRepository.findAllById(payslips.stream().map(EmployeePayslip::getEmployeeId).distinct().toList()),
                Employee::getId);
        Map<Long, String> branches = branchRepository.findAll().stream().collect(Collectors.toMap(Branch::getId, Branch::getName));
        Map<Long, String> agents = agentRepository.findAll().stream().collect(Collectors.toMap(Agent::getId, Agent::getName));

        List<Long> requestIds = settlements.values().stream()
                .map(AgentSettlement::getPaymentRequestId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, List<EmployeePayment>> paymentsByRequest = paymentRepository.findByPaymentRequestIdIn(requestIds).stream()
                .collect(Collectors.groupingBy(EmployeePayment::getPaymentRequestId));

        // What each run deducted, keyed by settlement and employee, so every payslip lists its own items.
        List<Long> settlementIds = List.copyOf(settlements.keySet());
        List<LoanRepayment> repayments = loanRepaymentRepository.findByPayrollRunIdIn(settlementIds);
        Map<Long, Loan> loans = byId(
                loanRepository.findAllById(repayments.stream().map(LoanRepayment::getLoanId).distinct().toList()), Loan::getId);
        Map<String, List<PayslipResponse.DeductionItem>> loanItems = repayments.stream()
                .filter(repayment -> loans.containsKey(repayment.getLoanId()))
                .collect(Collectors.groupingBy(
                        repayment -> key(repayment.getPayrollRunId(), loans.get(repayment.getLoanId()).getEmployeeId()),
                        Collectors.mapping(repayment -> new PayslipResponse.DeductionItem(
                                repayment.getLoanId(),
                                "Loan #" + repayment.getLoanId() + " instalment",
                                null,
                                repayment.getAmountPaid()), Collectors.toList())));
        Map<String, List<PayslipResponse.DeductionItem>> damageItems = damageRepository.findByPayrollRunIdIn(settlementIds).stream()
                .sorted(Comparator.comparing(Damage::getDamageDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(
                        damage -> key(damage.getPayrollRunId(), damage.getEmployeeId()),
                        Collectors.mapping(damage -> new PayslipResponse.DeductionItem(
                                damage.getId(),
                                "Damage: " + damage.getDescription(),
                                damage.getDamageDate(),
                                damage.getAmount()), Collectors.toList())));

        return payslips.stream()
                .map(payslip -> {
                    AgentSettlement settlement = settlements.get(payslip.getSettlementId());
                    List<EmployeePayment> payments = paymentsByRequest.getOrDefault(settlement.getPaymentRequestId(), List.of()).stream()
                            .filter(payment -> payment.getEmployeeId().equals(payslip.getEmployeeId()))
                            .toList();
                    String key = key(payslip.getSettlementId(), payslip.getEmployeeId());
                    return toResponse(payslip, settlement, employees.get(payslip.getEmployeeId()), branches, agents, payments,
                            loanItems.getOrDefault(key, List.of()), damageItems.getOrDefault(key, List.of()));
                })
                .sorted(Comparator.comparing(PayslipResponse::periodEnd).reversed()
                        .thenComparing(PayslipResponse::employeeName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private PayslipResponse toResponse(
            EmployeePayslip payslip,
            AgentSettlement settlement,
            Employee employee,
            Map<Long, String> branches,
            Map<Long, String> agents,
            List<EmployeePayment> payments,
            List<PayslipResponse.DeductionItem> loanItems,
            List<PayslipResponse.DeductionItem> damageItems
    ) {
        BigDecimal recorded = payments.stream().map(EmployeePayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PayslipResponse(
                payslip.getId(),
                settlement.getId(),
                settlement.getPeriodStart(),
                settlement.getPeriodEnd(),
                employee.getId(),
                employee.getEmployeeNumber(),
                employee.getFullName(),
                branches.get(employee.getBranchId()),
                agents.get(settlement.getAgentId()),
                payslip.getDaysWorked(),
                payslip.getDailyRate(),
                payslip.getPay(),
                payslip.getPaye(),
                payslip.getNssf(),
                payslip.getNita(),
                payslip.getLoan(),
                payslip.getDamage(),
                payslip.getAmountDue(),
                recorded,
                payslip.getAmountDue().subtract(recorded).max(BigDecimal.ZERO),
                payments.stream()
                        .map(payment -> new PayslipResponse.RecordedPayment(payment.getMpesaCode(), payment.getAmount(), payment.getPaidOn()))
                        .toList(),
                loanItems,
                damageItems
        );
    }

    private static String key(Long settlementId, Long employeeId) {
        return settlementId + ":" + employeeId;
    }

    private static <T> Map<Long, T> byId(List<T> rows, Function<T, Long> id) {
        return rows.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private static String orDash(String value) {
        return value != null ? value : "-";
    }

    private EmployeePayslip findOrThrow(Long id) {
        return payslipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + id));
    }
}
