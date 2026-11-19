// service/settlement/impl/AgentSettlementServiceImpl.java
package com.company.hrpayroll.service.settlement.impl;

import com.company.hrpayroll.dto.request.settlement.AgentSettlementRunRequest;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.settlement.AgentPaymentDetail;
import com.company.hrpayroll.dto.response.settlement.AgentPaymentReadinessResponse;
import com.company.hrpayroll.dto.response.settlement.AgentSettlementResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.agentadvance.AgentAdvance;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.offday.OffDay;
import com.company.hrpayroll.entity.settlement.AgentSettlement;
import com.company.hrpayroll.entity.settlement.EmployeePayslip;
import com.company.hrpayroll.entity.settlement.SettlementAdvanceRecovery;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.exception.SettlementInProgressException;
import com.company.hrpayroll.mapper.settlement.AgentSettlementMapper;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.agentadvance.AgentAdvanceRepository;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.company.CompanySettingsRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.offday.OffDayRepository;
import com.company.hrpayroll.repository.settlement.AgentSettlementRepository;
import com.company.hrpayroll.repository.settlement.EmployeePayslipRepository;
import com.company.hrpayroll.repository.settlement.SettlementAdvanceRecoveryRepository;
import com.company.hrpayroll.service.agentadvance.AgentAdvanceService;
import com.company.hrpayroll.service.common.pdf.DateLabel;
import com.company.hrpayroll.service.common.pdf.PdfDocumentWriter;
import com.company.hrpayroll.service.company.CompanyService;
import com.company.hrpayroll.service.damage.DamageService;
import com.company.hrpayroll.service.dayswork.DaysWorkedService;
import com.company.hrpayroll.service.loan.LoanService;
import com.company.hrpayroll.service.notification.NotificationService;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.company.hrpayroll.service.common.pdf.AmountFormatter.format;

/**
 * Agent payments. The Admin first previews a period (nothing is saved, nothing moves, agents see nothing), then
 * runs it. Preview and run use the very same calculation ({@link #plan}), so what is run is what was previewed.
 *
 * Each employee is paid their own recorded monthly salary for the days paid (days worked + off days); loans and
 * damages come off their pay; the agent's amount adds PAYE, NSSF, NITA, the agent fee and VAT, and takes off the
 * advances being recovered, each one listed with the date it was asked for and approved.
 */
@Service
@RequiredArgsConstructor
public class AgentSettlementServiceImpl implements AgentSettlementService {

    private static final String TYPE_INVOICE_READY = "AGENT_INVOICE_READY";
    private static final float[] EMPLOYEE_COLUMNS = {150f, 70f, 50f, 70f, 70f, 85f};

    private final AgentSettlementRepository settlementRepository;
    private final EmployeePayslipRepository payslipRepository;
    private final SettlementAdvanceRecoveryRepository recoveryRepository;
    private final AgentRepository agentRepository;
    private final AgentAdvanceRepository advanceRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final BranchRepository branchRepository;
    private final OffDayRepository offDayRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    private final AgentSettlementCalculator calculator;
    private final SettlementLockService lockService;
    private final DaysWorkedService daysWorkedService;
    private final LoanService loanService;
    private final DamageService damageService;
    private final AgentAdvanceService agentAdvanceService;
    private final PaymentRequestService paymentRequestService;
    private final NotificationService notificationService;
    private final AgentSettlementMapper mapper;
    private final CompanyService companyService;

    /** One agent's payment, worked out but not saved. */
    private record Plan(
            Agent agent,
            List<Employee> employees,
            List<AgentSettlementCalculator.EmployeeLine> lines,
            AgentSettlementCalculator.Totals totals,
            List<AgentAdvanceService.AdvanceRecovery> advances
    ) {}

    // ------------------------------------------------------------------ preview and run

    @Override
    @Transactional(readOnly = true)
    public List<AgentPaymentDetail> preview(AgentSettlementRunRequest request) {
        LocalDate start = request.periodStart();
        LocalDate end = request.periodEnd();
        Map<Long, String> branchNames = branchNames();
        return plan(start, end, request.agentIds()).stream()
                .map(plan -> detailOf(plan, start, end, branchNames))
                .toList();
    }

    // Single transaction: every agent's settlement commits, or none does. The advisory lock stops two
    // Admins recovering the same advance or deducting the same loan at the same moment.
    @Override
    @Transactional
    public List<AgentSettlementResponse> calculate(AgentSettlementRunRequest request, Long adminUserId) {
        if (!lockService.tryAcquire()) {
            throw new SettlementInProgressException(
                    "Another agent payment calculation is in progress. Please try again shortly.");
        }

        List<AgentSettlementResponse> results = new ArrayList<>();
        for (Plan plan : plan(request.periodStart(), request.periodEnd(), request.agentIds())) {
            AgentSettlement saved = save(plan, request.periodStart(), request.periodEnd(), adminUserId);
            results.add(mapper.toResponse(saved, plan.agent().getName()));
        }
        return results;
    }

    // The calculation itself, shared by preview and run. Reads only.
    private List<Plan> plan(LocalDate start, LocalDate end, List<Long> agentIds) {
        if (end.isBefore(start)) {
            throw new InvalidOperationException("Period end cannot be before period start");
        }

        Map<Long, List<Employee>> employeesByAgent = resolveEmployees(agentIds);
        if (employeesByAgent.isEmpty()) {
            throw new InvalidOperationException("No active agent has active employees to pay");
        }
        rejectAlreadyPaid(employeesByAgent.keySet(), start, end);

        List<Employee> employees = employeesByAgent.values().stream().flatMap(List::stream).toList();
        List<Long> ids = employees.stream().map(Employee::getId).toList();
        Map<Long, Integer> daysWorked = daysWorkedService.getDaysForPayment(ids, start, end);
        Map<Long, Integer> offDays = offDaysWithoutClockIn(ids, start, end);

        CompanySettings settings = companySettingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Company settings have not been configured yet"));
        int daysInMonth = start.lengthOfMonth();
        int periodDays = (int) ChronoUnit.DAYS.between(start, end) + 1;

        List<Plan> plans = new ArrayList<>();
        for (Map.Entry<Long, List<Employee>> entry : employeesByAgent.entrySet()) {
            Agent agent = findAgentOrThrow(entry.getKey());
            List<Employee> staff = entry.getValue().stream()
                    .sorted(Comparator.comparing(Employee::getFullName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            List<AgentSettlementCalculator.EmployeeLine> lines = staff.stream()
                    .map(employee -> calculator.calculateEmployee(
                            settings,
                            employee.getBasicSalary(),
                            daysWorked.getOrDefault(employee.getId(), 0),
                            offDays.getOrDefault(employee.getId(), 0),
                            daysInMonth,
                            periodDays,
                            loanService.getActiveDeductionsForEmployees(List.of(employee.getId())),
                            damageService.getApprovedDeductionsForEmployees(List.of(employee.getId()))))
                    .toList();
            AgentSettlementCalculator.Totals totals = calculator.calculateTotals(
                    agent, lines, settings.getVatRate(), agentAdvanceService.getRecoverableBalanceForAgent(agent.getId()));
            plans.add(new Plan(agent, staff, lines, totals, agentAdvanceService.planRecovery(agent.getId(), totals.advance())));
        }
        plans.sort(Comparator.comparing(plan -> plan.agent().getName(), String.CASE_INSENSITIVE_ORDER));
        return plans;
    }

    private AgentSettlement save(Plan plan, LocalDate start, LocalDate end, Long adminUserId) {
        Agent agent = plan.agent();
        AgentSettlementCalculator.Totals totals = plan.totals();

        AgentSettlement settlement = settlementRepository.save(AgentSettlement.builder()
                .agentId(agent.getId())
                .payBasis(AgentSettlement.BASIS_SALARY)
                .periodStart(start)
                .periodEnd(end)
                .employeeCount(plan.employees().size())
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

        for (int index = 0; index < plan.lines().size(); index++) {
            AgentSettlementCalculator.EmployeeLine line = plan.lines().get(index);
            payslipRepository.save(EmployeePayslip.builder()
                    .settlementId(settlement.getId())
                    .employeeId(plan.employees().get(index).getId())
                    .daysWorked(line.daysWorked())
                    .offDaysPaid(line.offDaysPaid())
                    .monthlySalary(line.monthlySalary())
                    .dailyRate(line.dailyRate())
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
        for (AgentSettlementCalculator.EmployeeLine line : plan.lines()) {
            line.loans().forEach(loan -> loanService.applyDeduction(loan.loanId(), settlement.getId(), loan.installmentAmount()));
            line.damages().forEach(damage -> damageService.markDeducted(damage.damageId(), settlement.getId()));
        }
        if (totals.advance().compareTo(BigDecimal.ZERO) > 0) {
            for (AgentAdvanceService.AdvanceRecovery step : agentAdvanceService.recoverForPayrollRun(agent.getId(), totals.advance())) {
                recoveryRepository.save(SettlementAdvanceRecovery.builder()
                        .settlementId(settlement.getId())
                        .advanceId(step.advanceId())
                        .amount(step.amount())
                        .balanceAfter(step.balanceAfter())
                        .build());
            }
        }

        if (totals.payable().compareTo(BigDecimal.ZERO) > 0) {
            String purpose = "Agent payment for " + DateLabel.range(start, end);
            settlement.setPaymentRequestId(paymentRequestService.createApproved(
                    agent.getId(), totals.payable(), purpose, adminUserId));
        }
        AgentSettlement saved = settlementRepository.save(settlement);

        // The agent's invoice is ready: tell them, so it reaches their end straight away.
        userRepository.findByAgentId(agent.getId()).ifPresent(user -> notificationService.notifyInApp(
                user.getId(), TYPE_INVOICE_READY,
                "Your invoice for " + DateLabel.range(start, end) + " is ready: KES " + format(totals.payable()) + ".",
                saved.getId()));
        return saved;
    }

    // Off days in the period on which the employee did not clock in (a day they came in anyway counts as worked).
    private Map<Long, Integer> offDaysWithoutClockIn(List<Long> employeeIds, LocalDate start, LocalDate end) {
        if (employeeIds.isEmpty()) return Map.of();
        Set<Long> wanted = new HashSet<>(employeeIds);
        Map<Long, Set<LocalDate>> attended = new HashMap<>();
        for (Attendance row : attendanceRepository.findByEmployeeIdInAndAttendanceDateBetween(employeeIds, start, end)) {
            if (row.getClockIn() != null) attended.computeIfAbsent(row.getEmployeeId(), k -> new HashSet<>()).add(row.getAttendanceDate());
        }
        Map<Long, Set<LocalDate>> off = new HashMap<>();
        for (OffDay row : offDayRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(end, start)) {
            if (!wanted.contains(row.getEmployeeId())) continue;
            LocalDate last = row.getEndDate().isAfter(end) ? end : row.getEndDate();
            for (LocalDate d = row.getStartDate().isBefore(start) ? start : row.getStartDate(); !d.isAfter(last); d = d.plusDays(1)) {
                if (!attended.getOrDefault(row.getEmployeeId(), Set.of()).contains(d)) {
                    off.computeIfAbsent(row.getEmployeeId(), k -> new HashSet<>()).add(d);
                }
            }
        }
        return off.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }

    // ------------------------------------------------------------------ reads

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

    // What was run, line by line, as stored at the time: the Admin and the agent read exactly this.
    @Override
    @Transactional(readOnly = true)
    public AgentPaymentDetail getDetail(Long settlementId) {
        AgentSettlement settlement = findOrThrow(settlementId);
        Agent agent = findAgentOrThrow(settlement.getAgentId());

        List<EmployeePayslip> payslips = payslipRepository.findBySettlementIdIn(List.of(settlementId));
        Map<Long, Employee> employees = employeeRepository.findAllById(payslips.stream().map(EmployeePayslip::getEmployeeId).toList())
                .stream().collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, String> branchNames = branchNames();

        List<AgentPaymentDetail.EmployeeLine> lines = payslips.stream()
                .map(p -> {
                    Employee e = employees.get(p.getEmployeeId());
                    return new AgentPaymentDetail.EmployeeLine(
                            p.getEmployeeId(),
                            e != null ? e.getEmployeeNumber() : null,
                            e != null ? e.getFullName() : "Employee #" + p.getEmployeeId(),
                            e != null ? branchNames.get(e.getBranchId()) : null,
                            p.getMonthlySalary(),
                            p.getDaysWorked() != null ? p.getDaysWorked() : 0,
                            p.getOffDaysPaid() != null ? p.getOffDaysPaid() : 0,
                            p.getDailyRate(),
                            p.getPay(),
                            p.getLoan(),
                            p.getDamage(),
                            netPay(settlement, p));
                })
                .sorted(Comparator.comparing(AgentPaymentDetail.EmployeeLine::employeeName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<SettlementAdvanceRecovery> recoveries = recoveryRepository.findBySettlementIdOrderByIdAsc(settlementId);
        List<AgentPaymentDetail.AdvanceLine> advances = advanceLines(recoveries.stream()
                .map(r -> new AgentAdvanceService.AdvanceRecovery(r.getAdvanceId(), r.getAmount(), r.getBalanceAfter()))
                .toList());
        List<String> warnings = new ArrayList<>();
        if (recoveries.isEmpty() && settlement.getAdvanceRecovered().compareTo(BigDecimal.ZERO) > 0) {
            warnings.add("This payment was run before each advance was recorded separately: KES "
                    + format(settlement.getAdvanceRecovered()) + " was taken back from the agent's advances in total.");
        }

        return new AgentPaymentDetail(
                settlement.getId(), agent.getId(), agent.getName(),
                settlement.getPeriodStart(), settlement.getPeriodEnd(), settlement.getPayBasis(),
                lines,
                settlement.getDaysPay(), settlement.getPayeTotal(), settlement.getNssfTotal(), settlement.getNitaTotal(),
                settlement.getAgentFee(), settlement.getVatRateUsed(), settlement.getVatAmount(),
                settlement.getLoanTotal(), settlement.getDamageTotal(),
                advances, settlement.getAdvanceRecovered(), settlement.getAmountPayable(),
                settlement.getPaymentRequestId(), warnings);
    }

    // The employee's net pay. Older (daily rate) runs stored the agent's share per employee, PAYE, NSSF and NITA
    // included; their own net pay is shown without them, the same way as now.
    private static BigDecimal netPay(AgentSettlement settlement, EmployeePayslip p) {
        if (AgentSettlement.BASIS_SALARY.equals(settlement.getPayBasis())) return p.getAmountDue();
        return p.getPay().subtract(p.getLoan()).subtract(p.getDamage()).max(BigDecimal.ZERO);
    }

    private AgentPaymentDetail detailOf(Plan plan, LocalDate start, LocalDate end, Map<Long, String> branchNames) {
        AgentSettlementCalculator.Totals totals = plan.totals();
        List<AgentPaymentDetail.EmployeeLine> lines = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (int index = 0; index < plan.lines().size(); index++) {
            Employee e = plan.employees().get(index);
            AgentSettlementCalculator.EmployeeLine line = plan.lines().get(index);
            lines.add(new AgentPaymentDetail.EmployeeLine(
                    e.getId(), e.getEmployeeNumber(), e.getFullName(), branchNames.get(e.getBranchId()),
                    line.monthlySalary(), line.daysWorked(), line.offDaysPaid(), line.dailyRate(),
                    line.pay(), line.loanTotal(), line.damageTotal(), line.amountDue()));
            if (e.getBasicSalary() == null || e.getBasicSalary().compareTo(BigDecimal.ZERO) <= 0) {
                warnings.add(e.getFullName() + " (" + e.getEmployeeNumber() + ") has no salary recorded, so is paid nothing. "
                        + "Add their salary on their employee page, then preview again.");
            }
        }
        return new AgentPaymentDetail(
                null, plan.agent().getId(), plan.agent().getName(), start, end, AgentSettlement.BASIS_SALARY,
                lines,
                totals.daysPay(), totals.paye(), totals.nssf(), totals.nita(), totals.agentFee(),
                totals.vatRate(), totals.vat(), totals.loans(), totals.damages(),
                advanceLines(plan.advances()), totals.advance(), totals.payable(),
                null, warnings);
    }

    private List<AgentPaymentDetail.AdvanceLine> advanceLines(List<AgentAdvanceService.AdvanceRecovery> steps) {
        if (steps.isEmpty()) return List.of();
        Map<Long, AgentAdvance> advances = advanceRepository.findAllById(
                        steps.stream().map(AgentAdvanceService.AdvanceRecovery::advanceId).toList())
                .stream().collect(Collectors.toMap(AgentAdvance::getId, Function.identity()));
        return steps.stream().map(step -> {
            AgentAdvance a = advances.get(step.advanceId());
            LocalDate requested = a == null ? null
                    : a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate() : a.getAdvanceDate();
            LocalDate approved = a == null ? null
                    : a.getDecidedAt() != null ? a.getDecidedAt().toLocalDate()
                    : a.isRequestedByAgent() ? null : requested;   // an advance the Admin made is approved when made
            return new AgentPaymentDetail.AdvanceLine(
                    step.advanceId(), a != null ? a.getPurpose() : null, a != null ? a.getAmount() : null,
                    requested, approved, step.amount(), step.balanceAfter());
        }).toList();
    }

    // Reads only. Uses the same rules as calculate, so the answer here is the answer it would give.
    @Override
    public AgentPaymentReadinessResponse getReadiness(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new InvalidOperationException("Period end cannot be before period start");
        }

        Map<Long, List<Employee>> employeesByAgent = resolveEmployees(null);
        List<Employee> employees = employeesByAgent.values().stream().flatMap(List::stream).toList();
        Map<Long, Integer> days = daysWorkedService.getDaysForPayment(
                employees.stream().map(Employee::getId).toList(), start, end);

        Map<Long, String> branchNames = branchNames();
        List<AgentPaymentReadinessResponse.BranchReadiness> branches = employees.stream()
                .collect(Collectors.groupingBy(Employee::getBranchId))
                .entrySet().stream()
                .map(entry -> new AgentPaymentReadinessResponse.BranchReadiness(
                        entry.getKey(),
                        branchNames.getOrDefault(entry.getKey(), "Branch #" + entry.getKey()),
                        entry.getValue().size(),
                        entry.getValue().stream().mapToInt(employee -> days.getOrDefault(employee.getId(), 0)).sum()))
                .sorted(Comparator.comparing(AgentPaymentReadinessResponse.BranchReadiness::branchName))
                .toList();

        List<String> alreadyPaid = employeesByAgent.isEmpty()
                ? List.of()
                : settlementRepository.findAgentIdsPaidForOverlappingPeriod(employeesByAgent.keySet(), start, end).stream()
                        .distinct()
                        .map(id -> findAgentOrThrow(id).getName())
                        .sorted()
                        .toList();

        boolean ready = !employees.isEmpty() && alreadyPaid.isEmpty();

        return new AgentPaymentReadinessResponse(start, end, ready, branches, alreadyPaid);
    }

    // ------------------------------------------------------------------ documents

    // The agent's invoice: every employee's pay and deductions, what is added on top, and every advance taken back.
    @Override
    public GeneratedDocument getInvoice(Long settlementId) {
        AgentPaymentDetail detail = getDetail(settlementId);
        Agent agent = findAgentOrThrow(detail.agentId());
        boolean salary = AgentSettlement.BASIS_SALARY.equals(detail.payBasis());

        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            writer.documentHeader(
                    "Agent Invoice",
                    List.of(
                            new PdfDocumentWriter.Field("Reference", "#" + detail.settlementId()),
                            new PdfDocumentWriter.Field("Period", DateLabel.range(detail.periodStart(), detail.periodEnd()))),
                    companyService.getLetterhead(),
                    List.of(
                            new PdfDocumentWriter.Field("Supplier", agent.getName()),
                            new PdfDocumentWriter.Field("KRA PIN", agent.getKraPin() != null ? agent.getKraPin() : "-"),
                            new PdfDocumentWriter.Field("Employees", String.valueOf(detail.employees().size()))));

            writer.sectionHeading("Employees");
            writer.tableRow(EMPLOYEE_COLUMNS, List.of("Employee", salary ? "Salary" : "Rate/day", "Days", "Pay", "Loans/Dmg", "Net pay"), true);
            for (AgentPaymentDetail.EmployeeLine line : detail.employees()) {
                String days = line.offDaysPaid() > 0 ? line.daysWorked() + "+" + line.offDaysPaid() + " off" : String.valueOf(line.daysWorked());
                BigDecimal taken = line.loan().add(line.damage());
                writer.tableRow(EMPLOYEE_COLUMNS, List.of(
                        line.employeeName(),
                        format(salary ? orZero(line.monthlySalary()) : orZero(line.dailyRate())),
                        days,
                        format(line.pay()),
                        minus(taken),
                        format(line.netPay())), false);
            }

            writeTotals(writer, detail, salary);
            writer.netBar("Total Amount Due", "KES " + format(detail.amountPayable()));
            for (String warning : detail.warnings()) writer.note(warning);
            writer.signatureBlock("Payment Details:", List.of("Released under Money Requests"), "Administrator", agent.getName());

            String fileName = "agent-invoice-" + agent.getId() + "-" + detail.settlementId() + ".pdf";
            return new GeneratedDocument(fileName, "application/pdf", writer.build());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate agent invoice PDF", e);
        }
    }

    @Override
    public GeneratedDocument getStatement(Long settlementId) {
        AgentPaymentDetail detail = getDetail(settlementId);
        Agent agent = findAgentOrThrow(detail.agentId());
        boolean salary = AgentSettlement.BASIS_SALARY.equals(detail.payBasis());

        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            writer.documentHeader(
                    "Payment Statement",
                    List.of(new PdfDocumentWriter.Field("Period", DateLabel.range(detail.periodStart(), detail.periodEnd()))),
                    companyService.getLetterhead(),
                    List.of(
                            new PdfDocumentWriter.Field("Agent", agent.getName()),
                            new PdfDocumentWriter.Field("Employees", String.valueOf(detail.employees().size()))));

            writeTotals(writer, detail, salary);
            writer.netBar("Amount Payable to Agent", "KES " + format(detail.amountPayable()));
            writer.note("PAYE, NSSF and NITA included above: KES "
                    + format(detail.paye().add(detail.nssf()).add(detail.nita()))
                    + ". The agent deducts these, with SHIF and any other statutory amounts, and pays them to the government.");

            String fileName = "agent-statement-" + agent.getId() + "-" + detail.settlementId() + ".pdf";
            return new GeneratedDocument(fileName, "application/pdf", writer.build());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate agent statement PDF", e);
        }
    }

    // What is added (salaries, statutory, fee, VAT) and what is taken off, each advance on its own line with a minus.
    private static void writeTotals(PdfDocumentWriter writer, AgentPaymentDetail detail, boolean salary) throws IOException {
        BigDecimal subtotal = detail.salaries().add(detail.paye()).add(detail.nssf()).add(detail.nita()).add(detail.agentFee());

        writer.sectionTitle("Invoice Details");
        writer.moneyHeader("Amount (KES)");
        int row = 1;
        writer.moneyRow(row++, salary ? "Salaries for the period" : "Pay for days worked", format(detail.salaries()));
        writer.moneyRow(row++, "PAYE", format(detail.paye()));
        writer.moneyRow(row++, "NSSF", format(detail.nssf()));
        writer.moneyRow(row++, "NITA", format(detail.nita()));
        writer.moneyRow(row++, "Agent fee", format(detail.agentFee()));
        writer.moneyRow(row, "VAT (" + format(detail.vatRate()) + "%)", format(detail.vat()));
        writer.moneyTotal("Total Before Deductions", "KES " + format(subtotal.add(detail.vat())));

        writer.sectionTitle("Deduction Details");
        row = 1;
        writer.moneyRow(row++, "Loans", minus(detail.loans()));
        writer.moneyRow(row++, "Damages", minus(detail.damages()));
        if (detail.advances().isEmpty()) {
            writer.moneyRow(row, "Advances", minus(detail.advanceRecovered()));
        } else {
            for (AgentPaymentDetail.AdvanceLine advance : detail.advances()) {
                writer.moneyRow(row++, "Advance #" + advance.advanceId() + " recovered", minus(advance.deducted()));
            }
        }
        writer.moneyTotal("Total Deductions", "KES " + minus(detail.loans().add(detail.damages()).add(detail.advanceRecovered())));
        for (AgentPaymentDetail.AdvanceLine advance : detail.advances()) {
            writer.note(advanceLabel(advance));
        }
    }

    // A deduction with a minus; nothing taken reads 0.00, never -0.00.
    static String minus(BigDecimal amount) {
        return amount != null && amount.signum() > 0 ? "-" + format(amount) : "0.00";
    }

    // "Advance #1 of KES 100,000.00: asked 03 Nov 2026, approved 05 Nov 2026. KES 21,786.80 taken off this payment,
    // KES 78,213.20 still owed."
    private static String advanceLabel(AgentPaymentDetail.AdvanceLine advance) {
        StringBuilder label = new StringBuilder("Advance #" + advance.advanceId());
        if (advance.advanceAmount() != null) label.append(" of KES ").append(format(advance.advanceAmount()));
        List<String> dates = new ArrayList<>();
        if (advance.requestedOn() != null) dates.add("asked " + DateLabel.format(advance.requestedOn()));
        if (advance.approvedOn() != null) dates.add("approved " + DateLabel.format(advance.approvedOn()));
        if (!dates.isEmpty()) label.append(": ").append(String.join(", ", dates));
        label.append(". KES ").append(format(advance.deducted())).append(" taken off this payment, KES ")
                .append(format(advance.balanceAfter())).append(" still owed.");
        return label.toString();
    }

    // ---- helpers ----

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Map<Long, String> branchNames() {
        return branchRepository.findAll().stream().collect(Collectors.toMap(Branch::getId, Branch::getName));
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
                alreadyPaid.size() + " agent(s) have already been paid for part of " + DateLabel.range(start, end)
                        + " (" + names + "). Choose a different period.");
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
