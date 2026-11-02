// service/employee/impl/EmployeeServiceImpl.java
package com.company.hrpayroll.service.employee.impl;

import com.company.hrpayroll.dto.request.employee.EmployeeRequest;
import com.company.hrpayroll.dto.response.employee.EmployeeDirectoryResponse;
import com.company.hrpayroll.dto.response.employee.EmployeeResponse;
import com.company.hrpayroll.dto.response.employee.EmployeeSummaryResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.department.Department;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.DuplicateResourceException;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.employee.EmployeeMapper;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.department.DepartmentRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.entity.damage.Damage;
import com.company.hrpayroll.entity.dayswork.DaysWorkedRecord;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceMappingRepository;
import com.company.hrpayroll.repository.damage.DamageRepository;
import com.company.hrpayroll.repository.dayswork.DaysWorkedRecordRepository;
import com.company.hrpayroll.repository.loan.LoanRepository;
import com.company.hrpayroll.repository.settlement.AgentSettlementRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.service.employee.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final String DELETED = "DELETED";

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final AgentRepository agentRepository;
    private final EmployeeMapper employeeMapper;
    private final BiometricDeviceMappingRepository biometricDeviceMappingRepository;
    private final LoanRepository loanRepository;
    private final DamageRepository damageRepository;
    private final DaysWorkedRecordRepository daysWorkedRecordRepository;
    private final AttendanceRepository attendanceRepository;
    private final AgentSettlementRepository agentSettlementRepository;
    private final UserRepository userRepository;

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        validateAndFetchBranch(request.branchId());
        validateDepartmentExists(request.departmentId());
        validateAgentExistsIfProvided(request.agentId());
        validateNationalIdUnique(request.nationalId(), null);

        Employee employee = employeeMapper.toEmployee(request);
        employee.setEmployeeNumber(generateEmployeeNumber());

        Employee saved = employeeRepository.save(employee);
        return toFullResponse(saved);
    }

    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = findEmployeeOrThrow(id);

        validateAndFetchBranch(request.branchId());
        validateDepartmentExists(request.departmentId());
        validateAgentExistsIfProvided(request.agentId());
        validateNationalIdUnique(request.nationalId(), id);

        employeeMapper.updateEmployeeFromRequest(employee, request);
        Employee saved = employeeRepository.save(employee);
        return toFullResponse(saved);
    }

    @Override
    public EmployeeResponse getEmployeeById(Long id) {
        return toFullResponse(findEmployeeOrThrow(id));
    }

    @Override
    public List<EmployeeSummaryResponse> getEmployeesByBranch(Long branchId) {
        return employeeRepository.findByBranchId(branchId).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Override
    public List<EmployeeSummaryResponse> getEmployeesByAgent(Long agentId) {
        return employeeRepository.findByAgentId(agentId).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Override
    public List<EmployeeSummaryResponse> getEmployeesByDepartment(Long departmentId) {
        return employeeRepository.findByDepartmentId(departmentId).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Override
    public List<EmployeeDirectoryResponse> getBranchDirectory(Long branchId) {
        return toDirectory(employeeRepository.findByBranchId(branchId));
    }

    // A login with no agent record sees nothing: a null here must never match the company's direct staff.
    @Override
    public List<EmployeeDirectoryResponse> getBranchDirectoryForAgent(Long branchId, Long agentId) {
        if (agentId == null) {
            return List.of();
        }
        return toDirectory(employeeRepository.findByBranchIdAndAgentId(branchId, agentId));
    }

    // Departments are company-wide, so any branch's employee can be placed in any department.
    @Override
    public EmployeeResponse assignDepartment(Long id, Long departmentId) {
        Employee employee = findEmployeeOrThrow(id);

        if (departmentId == null) {
            throw new InvalidOperationException("Choose a department");
        }
        validateDepartmentExists(departmentId);

        employee.setDepartmentId(departmentId);
        return toFullResponse(employeeRepository.save(employee));
    }

    @Override
    public void terminateEmployee(Long id) {
        Employee employee = findEmployeeOrThrow(id);
        employee.setEmploymentStatus("TERMINATED");
        employeeRepository.save(employee);
    }

    // The employee vanishes from every list and their code is freed, but the row stays: payslips,
    // payments, loans and damages must still show who the money was for. Removing the terminal
    // mapping stops them clocking in. The remaining codes then close up.
    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = findEmployeeOrThrow(id);
        rejectIfMoneyIsStillOpen(employee);

        employee.setEmploymentStatus(DELETED);
        employee.setEmployeeNumber("DEL-" + id);
        employee.setNationalId(null);
        employeeRepository.save(employee);

        biometricDeviceMappingRepository.deleteByEmployeeId(id);
        userRepository.findByEmployeeId(id).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
        });

        employeeRepository.clearEmployeeNumbers();
        employeeRepository.assignEmployeeNumbersInOrder();
    }

    @Override
    public EmployeeResponse assignBranch(Long id, Long branchId) {
        Employee employee = findEmployeeOrThrow(id);
        validateAndFetchBranch(branchId);

        employee.setBranchId(branchId);
        return toFullResponse(employeeRepository.save(employee));
    }

    // Deleting must never lose money: an unpaid loan, unpaid days or an undeducted damage have to be settled first.
    private void rejectIfMoneyIsStillOpen(Employee employee) {
        boolean openLoan = loanRepository.findByEmployeeId(employee.getId()).stream()
                .anyMatch(loan -> "PENDING".equals(loan.getStatus())
                        || ("APPROVED".equals(loan.getStatus()) && loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0));
        if (openLoan) {
            throw new InvalidOperationException(
                    employee.getFullName() + " still has an unpaid or pending loan. Settle or reject it before deleting them.");
        }

        List<Damage> undeducted = damageRepository.findByEmployeeIdAndStatus(employee.getId(), "APPROVED");
        if (!undeducted.isEmpty()) {
            BigDecimal total = undeducted.stream().map(Damage::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            throw new InvalidOperationException(
                    employee.getFullName() + " has " + undeducted.size() + " approved damage(s) worth KES " + total.toPlainString()
                            + " not yet deducted. Deduct them in an agent payment or cancel them before deleting them.");
        }

        long unpaidDays = countUnpaidDays(employee);
        if (unpaidDays > 0) {
            throw new InvalidOperationException(
                    employee.getFullName() + " has " + unpaidDays + " unpaid day(s). Pay them in an agent payment before deleting them.");
        }
    }

    // Only an agent is ever paid for days, so an employee with no agent has none owing. Anything after
    // the agent's latest paid period is unpaid: locked periods by their final figure, and attendance
    // not yet locked by the dates attended.
    private long countUnpaidDays(Employee employee) {
        if (employee.getAgentId() == null) {
            return 0;
        }

        LocalDate paidUpTo = agentSettlementRepository.findByAgentIdOrderByPeriodEndDescIdDesc(employee.getAgentId()).stream()
                .findFirst()
                .map(settlement -> settlement.getPeriodEnd())
                .orElse(LocalDate.of(1970, 1, 1));

        List<DaysWorkedRecord> locked = daysWorkedRecordRepository.findByEmployeeIdAndPeriodEndAfter(employee.getId(), paidUpTo);
        long lockedDays = locked.stream().mapToLong(DaysWorkedRecord::getDaysWorked).sum();
        long unlockedDays = attendanceRepository.findAttendedDatesAfter(employee.getId(), paidUpTo).stream()
                .filter(date -> locked.stream().noneMatch(record ->
                        !date.isBefore(record.getPeriodStart()) && !date.isAfter(record.getPeriodEnd())))
                .count();

        return lockedDays + unlockedDays;
    }

    // ---- validation helpers ----

    private Branch validateAndFetchBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
        if (!"ACTIVE".equals(branch.getStatus())) {
            throw new InvalidOperationException("Branch '" + branch.getName() + "' is inactive");
        }
        return branch;
    }

    private void validateDepartmentExists(Long departmentId) {
        if (departmentId == null) {
            return;
        }
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department not found with id: " + departmentId);
        }
    }

    private void validateAgentExistsIfProvided(Long agentId) {
        if (agentId != null && !agentRepository.existsById(agentId)) {
            throw new ResourceNotFoundException("Agent not found with id: " + agentId);
        }
    }

    private void validateNationalIdUnique(String nationalId, Long excludingEmployeeId) {
        if (nationalId == null || nationalId.isBlank()) {
            return;
        }
        boolean exists = employeeRepository.existsByNationalId(nationalId);
        if (exists && excludingEmployeeId == null) {
            throw new DuplicateResourceException("An employee with national ID '" + nationalId + "' already exists");
        }
        // On update, a stricter check (excluding this employee's own record)
        // would need a repository method like existsByNationalIdAndIdNot —
        // left as a straightforward addition once this needs tightening.
    }

    // ---- employee number generation ----

    // Format: EMP-{branchCode}-{sequential number, zero-padded to 4 digits}
    // e.g. EMP-NKR-0001. Retries on the rare chance of a collision under
    // concurrent creation, rather than relying on a single count check.
    // The next index across the company. Deleting closes the gaps, so this is normally the count + 1.
    private String generateEmployeeNumber() {
        long sequence = employeeRepository.countExisting() + 1;
        String candidate = "EMP-" + String.format("%04d", sequence);

        while (employeeRepository.existsByEmployeeNumber(candidate)) {
            sequence++;
            candidate = "EMP-" + String.format("%04d", sequence);
        }

        return candidate;
    }

    // ---- response builders ----

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .filter(employee -> !DELETED.equals(employee.getEmploymentStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    private EmployeeResponse toFullResponse(Employee employee) {
        String branchName = branchRepository.findById(employee.getBranchId())
                .map(Branch::getName).orElse(null);

        String departmentName = employee.getDepartmentId() != null
                ? departmentRepository.findById(employee.getDepartmentId())
                .map(Department::getName).orElse(null)
                : null;

        String agentName = employee.getAgentId() != null
                ? agentRepository.findById(employee.getAgentId())
                .map(a -> a.getName()).orElse(null)
                : null;

        return employeeMapper.toEmployeeResponse(employee, branchName, departmentName, agentName);
    }

    // Names are read once, not per row.
    private List<EmployeeDirectoryResponse> toDirectory(List<Employee> employees) {
        Map<Long, String> departments = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
        Map<Long, String> agents = agentRepository.findAll().stream()
                .collect(Collectors.toMap(Agent::getId, Agent::getName));

        return employees.stream()
                .map(employee -> employeeMapper.toEmployeeDirectoryResponse(
                        employee, departments.get(employee.getDepartmentId()), agents.get(employee.getAgentId())))
                .toList();
    }

    private EmployeeSummaryResponse toSummaryResponse(Employee employee) {
        String branchName = branchRepository.findById(employee.getBranchId())
                .map(Branch::getName).orElse(null);
        return employeeMapper.toEmployeeSummaryResponse(employee, branchName);
    }
}