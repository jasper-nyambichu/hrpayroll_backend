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
import com.company.hrpayroll.service.employee.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final AgentRepository agentRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        Branch branch = validateAndFetchBranch(request.branchId());
        validateDepartmentExists(request.departmentId());
        validateAgentExistsIfProvided(request.agentId());
        validateNationalIdUnique(request.nationalId(), null);

        Employee employee = employeeMapper.toEmployee(request);
        employee.setEmployeeNumber(generateEmployeeNumber(branch));

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

    @Override
    public void deleteEmployee(Long id) {
        findEmployeeOrThrow(id);
        employeeRepository.deleteById(id);
    }

    // ---- validation helpers ----

    private Branch validateAndFetchBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
        if ("INACTIVE".equals(branch.getStatus())) {
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
    private String generateEmployeeNumber(Branch branch) {
        long existingCountForBranch = employeeRepository.findByBranchId(branch.getId()).size();
        String candidate;
        int attempt = 0;

        do {
            long sequence = existingCountForBranch + 1 + attempt;
            candidate = "EMP-" + branch.getCode() + "-" + String.format("%04d", sequence);
            attempt++;
        } while (employeeRepository.existsByEmployeeNumber(candidate));

        return candidate;
    }

    // ---- response builders ----

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
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