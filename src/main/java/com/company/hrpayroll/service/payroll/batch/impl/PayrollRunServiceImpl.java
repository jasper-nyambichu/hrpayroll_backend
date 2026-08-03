// service/payroll/batch/impl/PayrollRunServiceImpl.java
package com.company.hrpayroll.service.payroll.batch.impl;

import com.company.hrpayroll.dto.request.payroll.PayrollRunRequest;
import com.company.hrpayroll.dto.response.payroll.AgentPayrollSummaryResponse;
import com.company.hrpayroll.dto.response.payroll.PayrollRunResponse;
import com.company.hrpayroll.dto.response.payroll.PayslipResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.payroll.AgentPayrollSummary;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.entity.payroll.PayrollRunStatus;
import com.company.hrpayroll.entity.payroll.Payslip;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.payroll.AgentPayrollSummaryMapper;
import com.company.hrpayroll.mapper.payroll.PayrollRunMapper;
import com.company.hrpayroll.mapper.payroll.PayslipMapper;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.payroll.AgentPayrollSummaryRepository;
import com.company.hrpayroll.repository.payroll.PayrollRunRepository;
import com.company.hrpayroll.repository.payroll.PayslipRepository;
import com.company.hrpayroll.service.agentadvance.AgentAdvanceService;
import com.company.hrpayroll.service.loan.LoanService;
import com.company.hrpayroll.service.payroll.agentformula.AgentPayrollCalculationService;
import com.company.hrpayroll.service.payroll.batch.PayrollRunService;
import com.company.hrpayroll.service.payroll.engine.PayrollCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollRunServiceImpl implements PayrollRunService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final AgentPayrollSummaryRepository agentPayrollSummaryRepository;
    private final EmployeeRepository employeeRepository;
    private final AgentRepository agentRepository;

    private final PayrollCalculationService payrollCalculationService;
    private final AgentPayrollCalculationService agentPayrollCalculationService;
    private final LoanService loanService;
    private final AgentAdvanceService agentAdvanceService;

    private final PayrollRunMapper payrollRunMapper;
    private final PayslipMapper payslipMapper;
    private final AgentPayrollSummaryMapper agentPayrollSummaryMapper;

    // Single transaction: the whole run either commits completely, or none
    // of it is saved — no PayrollRun row is left in a half-finished state,
    // which is exactly the "safe money handling" guarantee Spring Boot was
    // chosen for. For very large employee counts (1,000+), this is the spot
    // where Spring Batch chunking would replace the in-memory loop below —
    // same business logic, processed in restartable chunks instead of one
    // long transaction.
    @Override
    @Transactional
    public PayrollRunResponse runPayroll(PayrollRunRequest request, Long initiatedByUserId) {
        validateRequest(request);

        List<Employee> employees = resolveEmployees(request);
        if (employees.isEmpty()) {
            throw new InvalidOperationException("No active employees match the selected payroll scope");
        }

        PayrollRun run = PayrollRun.builder()
                .periodStart(request.periodStart())
                .periodEnd(request.periodEnd())
                .scopeBranchIds(joinIds(request.branchIds()))
                .scopeAgentIds(joinIds(request.agentIds()))
                .scopeDepartmentIds(joinIds(request.departmentIds()))
                .status(PayrollRunStatus.IN_PROGRESS)
                .initiatedByUserId(initiatedByUserId)
                .build();
        run = payrollRunRepository.save(run);

        // 1. Calculate and save a payslip per employee, applying any loan
        //    deductions it produced.
        List<Payslip> payslips = new ArrayList<>();
        for (Employee employee : employees) {
            Payslip payslip = payrollCalculationService.calculateForEmployee(employee, run);
            Payslip savedPayslip = payslipRepository.save(payslip);
            payslips.add(savedPayslip);
            applyLoanDeductionsFor(employee, run);
        }

        // 2. Group employees by agent and build one AgentPayrollSummary per
        //    agent, recovering advances only after the summary is built.
        List<AgentPayrollSummary> agentSummaries = calculateAndSaveAgentSummaries(employees, payslips, run);

        // 3. Update run totals and mark complete.
        BigDecimal totalGross = payslips.stream().map(Payslip::getGrossPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = payslips.stream().map(Payslip::getNetPay).reduce(BigDecimal.ZERO, BigDecimal::add);

        run.setTotalGrossPayroll(totalGross);
        run.setTotalNetPayroll(totalNet);
        run.setStatus(PayrollRunStatus.COMPLETED);
        run = payrollRunRepository.save(run);

        return payrollRunMapper.toPayrollRunResponse(run, payslips.size(), agentSummaries.size());
    }

    @Override
    public PayrollRunResponse getRunById(Long payrollRunId) {
        PayrollRun run = findRunOrThrow(payrollRunId);
        int payslipCount = payslipRepository.findByPayrollRunId(payrollRunId).size();
        int agentCount = agentPayrollSummaryRepository.findByPayrollRunId(payrollRunId).size();
        return payrollRunMapper.toPayrollRunResponse(run, payslipCount, agentCount);
    }

    @Override
    public List<PayslipResponse> getPayslipsForRun(Long payrollRunId) {
        return payslipRepository.findByPayrollRunId(payrollRunId).stream()
                .map(payslip -> {
                    String employeeName = employeeRepository.findById(payslip.getEmployeeId())
                            .map(Employee::getFullName).orElse(null);
                    return payslipMapper.toPayslipResponse(payslip, employeeName);
                })
                .toList();
    }

    @Override
    public List<AgentPayrollSummaryResponse> getAgentSummariesForRun(Long payrollRunId) {
        return agentPayrollSummaryRepository.findByPayrollRunId(payrollRunId).stream()
                .map(summary -> {
                    String agentName = agentRepository.findById(summary.getAgentId())
                            .map(Agent::getName).orElse(null);
                    return agentPayrollSummaryMapper.toAgentPayrollSummaryResponse(summary, agentName);
                })
                .toList();
    }

    // ---- orchestration helpers ----

    private void applyLoanDeductionsFor(Employee employee, PayrollRun run) {
        loanService.getActiveDeductionsForEmployees(List.of(employee.getId()))
                .forEach(deduction -> loanService.applyDeduction(
                        deduction.loanId(), run.getId(), deduction.installmentAmount()));
    }

    private List<AgentPayrollSummary> calculateAndSaveAgentSummaries(
            List<Employee> employees, List<Payslip> payslips, PayrollRun run) {

        Set<Long> agentIds = employees.stream()
                .map(Employee::getAgentId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<AgentPayrollSummary> summaries = new ArrayList<>();

        for (Long agentId : agentIds) {
            Agent agent = agentRepository.findById(agentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + agentId));

            List<Employee> agentEmployees = employees.stream()
                    .filter(e -> agentId.equals(e.getAgentId()))
                    .toList();
            Set<Long> agentEmployeeIds = agentEmployees.stream().map(Employee::getId).collect(Collectors.toSet());

            List<Payslip> agentPayslips = payslips.stream()
                    .filter(p -> agentEmployeeIds.contains(p.getEmployeeId()))
                    .toList();

            BigDecimal recoverableBalance = agentAdvanceService.getRecoverableBalanceForAgent(agentId);

            AgentPayrollSummary summary = agentPayrollCalculationService.calculateForAgent(
                    agent, agentEmployees, agentPayslips, recoverableBalance, run);

            AgentPayrollSummary saved = agentPayrollSummaryRepository.save(summary);
            summaries.add(saved);

            // Only now, after the summary is safely saved, actually mark the
            // advance as recovered — mirrors the "calculate first, mutate
            // state deliberately after" pattern used throughout.
            if (saved.getAdvanceRecoveryTotal().compareTo(BigDecimal.ZERO) > 0) {
                agentAdvanceService.recoverForPayrollRun(agentId, saved.getAdvanceRecoveryTotal());
            }
        }

        return summaries;
    }

    private List<Employee> resolveEmployees(PayrollRunRequest request) {
        if (request.allEmployees()) {
            return employeeRepository.findByEmploymentStatus("ACTIVE");
        }

        Set<Employee> resolved = new HashSet<>();

        if (request.branchIds() != null && !request.branchIds().isEmpty()) {
            resolved.addAll(employeeRepository.findByBranchIdIn(request.branchIds()));
        }
        if (request.agentIds() != null && !request.agentIds().isEmpty()) {
            resolved.addAll(employeeRepository.findByAgentIdIn(request.agentIds()));
        }
        if (request.departmentIds() != null) {
            request.departmentIds().forEach(deptId ->
                    resolved.addAll(employeeRepository.findByDepartmentId(deptId)));
        }

        return resolved.stream()
                .filter(e -> "ACTIVE".equals(e.getEmploymentStatus()))
                .toList();
    }

    private void validateRequest(PayrollRunRequest request) {
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new InvalidOperationException("Period end cannot be before period start");
        }

        boolean hasScope = request.allEmployees()
                || (request.branchIds() != null && !request.branchIds().isEmpty())
                || (request.agentIds() != null && !request.agentIds().isEmpty())
                || (request.departmentIds() != null && !request.departmentIds().isEmpty());

        if (!hasScope) {
            throw new InvalidOperationException(
                    "Select 'all employees' or at least one branch, agent, or department");
        }
    }

    private PayrollRun findRunOrThrow(Long id) {
        return payrollRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found with id: " + id));
    }

    private String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}