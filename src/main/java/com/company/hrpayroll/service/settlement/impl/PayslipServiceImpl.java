// service/settlement/impl/PayslipServiceImpl.java
package com.company.hrpayroll.service.settlement.impl;

import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.settlement.PayslipResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.payment.EmployeePayment;
import com.company.hrpayroll.entity.settlement.AgentSettlement;
import com.company.hrpayroll.entity.settlement.EmployeePayslip;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.payment.EmployeePaymentRepository;
import com.company.hrpayroll.repository.settlement.AgentSettlementRepository;
import com.company.hrpayroll.repository.settlement.EmployeePayslipRepository;
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

    private final EmployeePayslipRepository payslipRepository;
    private final AgentSettlementRepository settlementRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeePaymentRepository paymentRepository;
    private final BranchRepository branchRepository;
    private final AgentRepository agentRepository;
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
            companyService.writeLetterhead(writer);
            writer.title("Payslip");
            writer.subtitle(payslip.employeeName() + " (" + payslip.employeeNumber() + ")");
            writer.subtitle("Period: " + payslip.periodStart() + " to " + payslip.periodEnd());
            writer.subtitle("Branch: " + orDash(payslip.branchName()) + "   Agent: " + orDash(payslip.agentName()));
            writer.divider();

            writer.sectionHeading("Earnings");
            writer.row(payslip.daysWorked() + " days x " + format(payslip.dailyRate()), format(payslip.pay()));
            writer.row("PAYE", format(payslip.paye()));
            writer.row("NSSF", format(payslip.nssf()));
            writer.row("NITA", format(payslip.nita()));

            writer.sectionHeading("Deductions");
            writer.row("Loans", "(" + format(payslip.loan()) + ")");
            writer.row("Damages", "(" + format(payslip.damage()) + ")");

            writer.divider();
            writer.boldRow("Amount Paid to Employee", format(payslip.amountDue()));

            writer.sectionHeading("Payments Recorded by the Agent");
            if (payslip.payments().isEmpty()) {
                writer.row("None recorded yet", "");
            }
            for (PayslipResponse.RecordedPayment payment : payslip.payments()) {
                writer.row(payment.paidOn() + "   M-Pesa " + payment.mpesaCode(), format(payment.amount()));
            }
            writer.divider();
            writer.row("Total recorded", format(payslip.amountRecorded()));
            writer.boldRow("Not yet recorded", format(payslip.amountNotRecorded()));

            String fileName = "payslip-" + payslip.employeeNumber() + "-" + payslip.periodEnd() + ".pdf";
            return new GeneratedDocument(fileName, "application/pdf", writer.build());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate payslip PDF", e);
        }
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

        return payslips.stream()
                .map(payslip -> {
                    AgentSettlement settlement = settlements.get(payslip.getSettlementId());
                    List<EmployeePayment> payments = paymentsByRequest.getOrDefault(settlement.getPaymentRequestId(), List.of()).stream()
                            .filter(payment -> payment.getEmployeeId().equals(payslip.getEmployeeId()))
                            .toList();
                    return toResponse(payslip, settlement, employees.get(payslip.getEmployeeId()), branches, agents, payments);
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
            List<EmployeePayment> payments
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
                        .toList()
        );
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
