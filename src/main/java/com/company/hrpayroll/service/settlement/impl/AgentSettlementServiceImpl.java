// service/settlement/impl/AgentSettlementServiceImpl.java
package com.company.hrpayroll.service.settlement.impl;

import com.company.hrpayroll.dto.request.settlement.AgentSettlementRunRequest;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.settlement.AgentSettlementResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.settlement.AgentSettlement;
import com.company.hrpayroll.entity.settlement.EmployeePayslip;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.exception.SettlementInProgressException;
import com.company.hrpayroll.mapper.settlement.AgentSettlementMapper;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.company.CompanySettingsRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.settlement.AgentSettlementRepository;
import com.company.hrpayroll.repository.settlement.EmployeePayslipRepository;
import com.company.hrpayroll.service.agentadvance.AgentAdvanceService;
import com.company.hrpayroll.service.common.pdf.PdfDocumentWriter;
import com.company.hrpayroll.service.company.CompanyService;
import com.company.hrpayroll.service.damage.DamageService;
import com.company.hrpayroll.service.dayswork.DaysWorkedService;
import com.company.hrpayroll.service.loan.LoanService;
import com.company.hrpayroll.service.payment.PaymentRequestService;
import com.company.hrpayroll.service.settlement.AgentSettlementCalculator;
import com.company.hrpayroll.service.settlement.AgentSettlementService;
import com.company.hrpayroll.service.settlement.SettlementLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.company.hrpayroll.service.common.pdf.AmountFormatter.format;

@Service
@RequiredArgsConstructor
public class AgentSettlementServiceImpl implements AgentSettlementService {

    private final AgentSettlementRepository settlementRepository;
    private final EmployeePayslipRepository payslipRepository;
    private final AgentRepository agentRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanySettingsRepository companySettingsRepository;

    private final AgentSettlementCalculator calculator;
    private final SettlementLockService lockService;
    private final DaysWorkedService daysWorkedService;
    private final LoanService loanService;
    private final DamageService damageService;
    private final AgentAdvanceService agentAdvanceService;
    private final PaymentRequestService paymentRequestService;
    private final AgentSettlementMapper mapper;
    private final CompanyService companyService;

    // Single transaction: every agent's settlement commits, or none does. The advisory lock stops two
    // Admins recovering the same advance or deducting the same loan at the same moment.
    @Override
    @Transactional
    public List<AgentSettlementResponse> calculate(AgentSettlementRunRequest request, Long adminUserId) {
        if (!lockService.tryAcquire()) {
            throw new SettlementInProgressException(
                    "Another agent payment calculation is in progress. Please try again shortly.");
        }

        LocalDate start = request.periodStart();
        LocalDate end = request.periodEnd();
        if (end.isBefore(start)) {
            throw new InvalidOperationException("Period end cannot be before period start");
        }

        Map<Long, List<Employee>> employeesByAgent = resolveEmployees(request.agentIds());
        if (employeesByAgent.isEmpty()) {
            throw new InvalidOperationException("No active agent has active employees to pay");
        }

        rejectAlreadyPaid(employeesByAgent.keySet(), start, end);

        List<Employee> employees = employeesByAgent.values().stream().flatMap(List::stream).toList();
        Map<Long, Integer> lockedDays = requireLockedDays(employees, start, end);

        CompanySettings settings = companySettingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Company settings have not been configured yet"));

        List<AgentSettlementResponse> results = new ArrayList<>();
        for (Map.Entry<Long, List<Employee>> entry : employeesByAgent.entrySet()) {
            Agent agent = findAgentOrThrow(entry.getKey());
            AgentSettlement saved = settleAgent(agent, entry.getValue(), lockedDays, settings, start, end, adminUserId);
            results.add(mapper.toResponse(saved, agent.getName()));
        }
        return results;
    }

    private AgentSettlement settleAgent(
            Agent agent,
            List<Employee> employees,
            Map<Long, Integer> lockedDays,
            CompanySettings settings,
            LocalDate start,
            LocalDate end,
            Long adminUserId
    ) {
        List<AgentSettlementCalculator.EmployeeLine> lines = employees.stream()
                .map(employee -> calculator.calculateEmployee(
                        settings,
                        lockedDays.get(employee.getId()),
                        loanService.getActiveDeductionsForEmployees(List.of(employee.getId())),
                        damageService.getApprovedDeductionsForEmployees(List.of(employee.getId()))))
                .toList();

        AgentSettlementCalculator.Totals totals = calculator.calculateTotals(
                agent, lines, settings.getVatRate(), agentAdvanceService.getRecoverableBalanceForAgent(agent.getId()));

        AgentSettlement settlement = settlementRepository.save(AgentSettlement.builder()
                .agentId(agent.getId())
                .periodStart(start)
                .periodEnd(end)
                .employeeCount(employees.size())
                .daysWorkedTotal(totals.daysWorked())
                .daysPay(totals.daysPay())
                .payeTotal(totals.paye())
                .nssfTotal(totals.nssf())
                .nitaTotal(totals.nita())
                .agentFee(totals.agentFee())
                .vatRateUsed(totals.vatRate())
                .vatAmount(totals.vat())
                .loanTotal(totals.loans())
                .damageTotal(totals.damages())
                .advanceRecovered(totals.advance())
                .amountPayable(totals.payable())
                .calculatedByUserId(adminUserId)
                .build());

        for (int index = 0; index < lines.size(); index++) {
            AgentSettlementCalculator.EmployeeLine line = lines.get(index);
            payslipRepository.save(EmployeePayslip.builder()
                    .settlementId(settlement.getId())
                    .employeeId(employees.get(index).getId())
                    .daysWorked(line.daysWorked())
                    .dailyRate(settings.getDailyRate())
                    .pay(line.pay())
                    .paye(line.paye())
                    .nssf(line.nssf())
                    .nita(line.nita())
                    .loan(line.loanTotal())
                    .damage(line.damageTotal())
                    .amountDue(line.amountDue())
                    .build());
        }

        // Only now that the settlement is saved do the balances actually move.
        for (AgentSettlementCalculator.EmployeeLine line : lines) {
            line.loans().forEach(loan -> loanService.applyDeduction(loan.loanId(), settlement.getId(), loan.installmentAmount()));
            line.damages().forEach(damage -> damageService.markDeducted(damage.damageId(), settlement.getId()));
        }
        if (totals.advance().compareTo(BigDecimal.ZERO) > 0) {
            agentAdvanceService.recoverForPayrollRun(agent.getId(), totals.advance());
        }

        if (totals.payable().compareTo(BigDecimal.ZERO) > 0) {
            String purpose = "Agent payment for " + start + " to " + end;
            settlement.setPaymentRequestId(paymentRequestService.createApproved(
                    agent.getId(), totals.payable(), purpose, adminUserId));
        }

        return settlementRepository.save(settlement);
    }

    @Override
    public List<AgentSettlementResponse> getAll() {
        return settlementRepository.findAllByOrderByPeriodEndDescIdDesc().stream().map(this::toResponse).toList();
    }

    @Override
    public List<AgentSettlementResponse> getForAgent(Long agentId) {
        return settlementRepository.findByAgentIdOrderByPeriodEndDescIdDesc(agentId).stream().map(this::toResponse).toList();
    }

    @Override
    public AgentSettlementResponse getById(Long settlementId) {
        return toResponse(findOrThrow(settlementId));
    }

    @Override
    public GeneratedDocument getInvoice(Long settlementId) {
        AgentSettlement settlement = findOrThrow(settlementId);
        Agent agent = findAgentOrThrow(settlement.getAgentId());
        BigDecimal subtotal = subtotalBeforeVat(settlement);

        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            companyService.writeLetterhead(writer);
            writer.title("Agent Invoice");
            writer.subtitle("Reference: #" + settlement.getId());
            writer.subtitle("Agent KRA PIN: " + (agent.getKraPin() != null ? agent.getKraPin() : "-"));
            writer.divider();

            writer.row("Supplier", agent.getName());
            writer.row("Period", settlement.getPeriodStart() + " to " + settlement.getPeriodEnd());
            writer.divider();

            writer.row("Amount Before VAT", format(subtotal));
            writer.row("VAT (" + format(settlement.getVatRateUsed()) + "%)", format(settlement.getVatAmount()));
            writer.row("Less: Loans", "(" + format(settlement.getLoanTotal()) + ")");
            writer.row("Less: Damages", "(" + format(settlement.getDamageTotal()) + ")");
            writer.row("Less: Advances", "(" + format(settlement.getAdvanceRecovered()) + ")");

            writer.divider();
            writer.boldRow("Total Amount Due", format(settlement.getAmountPayable()));

            String fileName = "agent-invoice-" + agent.getId() + "-" + settlement.getId() + ".pdf";
            return new GeneratedDocument(fileName, "application/pdf", writer.build());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate agent invoice PDF", e);
        }
    }

    @Override
    public GeneratedDocument getStatement(Long settlementId) {
        AgentSettlement settlement = findOrThrow(settlementId);
        Agent agent = findAgentOrThrow(settlement.getAgentId());

        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            companyService.writeLetterhead(writer);
            writer.title("Agent Payment Statement");
            writer.subtitle("Agent: " + agent.getName());
            writer.subtitle("Period: " + settlement.getPeriodStart() + " to " + settlement.getPeriodEnd());
            writer.subtitle("Employees: " + settlement.getEmployeeCount() + "   Days worked: " + settlement.getDaysWorkedTotal());
            writer.divider();

            writer.sectionHeading("Additions");
            writer.row("Pay for days worked", format(settlement.getDaysPay()));
            writer.row("PAYE", format(settlement.getPayeTotal()));
            writer.row("NSSF", format(settlement.getNssfTotal()));
            writer.row("NITA", format(settlement.getNitaTotal()));
            writer.row("Agent fee", format(settlement.getAgentFee()));
            writer.row("VAT (" + format(settlement.getVatRateUsed()) + "%)", format(settlement.getVatAmount()));

            writer.sectionHeading("Deductions");
            writer.row("Loans", "(" + format(settlement.getLoanTotal()) + ")");
            writer.row("Damages", "(" + format(settlement.getDamageTotal()) + ")");
            writer.row("Advances", "(" + format(settlement.getAdvanceRecovered()) + ")");

            writer.divider();
            writer.boldRow("Amount Payable to Agent", format(settlement.getAmountPayable()));

            String fileName = "agent-statement-" + agent.getId() + "-" + settlement.getId() + ".pdf";
            return new GeneratedDocument(fileName, "application/pdf", writer.build());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate agent statement PDF", e);
        }
    }

    // ---- helpers ----

    private BigDecimal subtotalBeforeVat(AgentSettlement settlement) {
        return settlement.getDaysPay()
                .add(settlement.getPayeTotal())
                .add(settlement.getNssfTotal())
                .add(settlement.getNitaTotal())
                .add(settlement.getAgentFee());
    }

    // Only staff with an agent are paid this way; agent id -> that agent's active employees.
    private Map<Long, List<Employee>> resolveEmployees(List<Long> requestedAgentIds) {
        Set<Long> activeAgentIds = requestedAgentIds == null || requestedAgentIds.isEmpty()
                ? agentRepository.findAll().stream()
                        .filter(agent -> "ACTIVE".equals(agent.getStatus()))
                        .map(Agent::getId)
                        .collect(Collectors.toSet())
                : requestedAgentIds.stream()
                        .map(this::findAgentOrThrow)
                        .filter(agent -> "ACTIVE".equals(agent.getStatus()))
                        .map(Agent::getId)
                        .collect(Collectors.toSet());

        if (activeAgentIds.isEmpty()) {
            return Map.of();
        }

        return employeeRepository.findByAgentIdIn(new ArrayList<>(activeAgentIds)).stream()
                .filter(employee -> "ACTIVE".equals(employee.getEmploymentStatus()))
                .collect(Collectors.groupingBy(Employee::getAgentId));
    }

    private void rejectAlreadyPaid(Set<Long> agentIds, LocalDate start, LocalDate end) {
        Set<Long> alreadyPaid = new HashSet<>(settlementRepository.findAgentIdsPaidForOverlappingPeriod(agentIds, start, end));
        if (alreadyPaid.isEmpty()) {
            return;
        }

        String names = alreadyPaid.stream()
                .limit(3)
                .map(id -> findAgentOrThrow(id).getName())
                .collect(Collectors.joining(", "));

        throw new InvalidOperationException(
                alreadyPaid.size() + " agent(s) have already been paid for part of " + start + " to " + end
                        + " (" + names + "). Choose a different period.");
    }

    // Every employee needs a locked days-worked figure for exactly this period.
    private Map<Long, Integer> requireLockedDays(List<Employee> employees, LocalDate start, LocalDate end) {
        List<Long> employeeIds = employees.stream().map(Employee::getId).toList();
        Map<Long, Integer> locked = daysWorkedService.getLockedDays(employeeIds, start, end);

        List<Employee> missing = employees.stream()
                .filter(employee -> !locked.containsKey(employee.getId()))
                .toList();
        if (missing.isEmpty()) {
            return locked;
        }

        String examples = missing.stream()
                .limit(3)
                .map(Employee::getEmployeeNumber)
                .collect(Collectors.joining(", "));

        throw new InvalidOperationException(
                missing.size() + " employee(s) have no locked days worked for " + start + " to " + end
                        + " (e.g. " + examples + "). The Back Office must lock the days for those branches "
                        + "for exactly this period first.");
    }

    private AgentSettlement findOrThrow(Long id) {
        return settlementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent payment not found with id: " + id));
    }

    private Agent findAgentOrThrow(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + id));
    }

    private AgentSettlementResponse toResponse(AgentSettlement settlement) {
        String agentName = agentRepository.findById(settlement.getAgentId()).map(Agent::getName).orElse(null);
        return mapper.toResponse(settlement, agentName);
    }
}
