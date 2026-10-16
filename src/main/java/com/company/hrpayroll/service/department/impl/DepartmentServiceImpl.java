// service/department/impl/DepartmentServiceImpl.java
package com.company.hrpayroll.service.department.impl;

import com.company.hrpayroll.dto.request.department.DepartmentRequest;
import com.company.hrpayroll.dto.response.department.DepartmentResponse;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.department.Department;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.DuplicateResourceException;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.department.DepartmentMapper;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.department.DepartmentRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.department.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        DepartmentRequest normalized = normalize(request);

        // A department must belong to a branch that actually exists and is active.
        Branch branch = findBranchOrThrow(normalized.branchId());
        if ("INACTIVE".equals(branch.getStatus())) {
            throw new InvalidOperationException("Cannot add a department to an inactive branch");
        }

        if (departmentRepository.existsByBranchIdAndNameIgnoreCase(normalized.branchId(), normalized.name())) {
            throw new DuplicateResourceException(
                    "Branch '" + branch.getName() + "' already has a department named '" + normalized.name() + "'");
        }
        validateHead(normalized.headOfDepartmentId(), normalized.branchId());

        Department saved = departmentRepository.save(departmentMapper.toDepartment(normalized));
        return toResponseWithNames(saved);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = findDepartmentOrThrow(id);
        DepartmentRequest normalized = normalize(request);

        Branch branch = findBranchOrThrow(normalized.branchId());

        // Moving a department to another branch would strand its employees in a
        // department that no longer belongs to their branch.
        boolean branchChanging = !department.getBranchId().equals(normalized.branchId());
        if (branchChanging && !employeeRepository.findByDepartmentId(id).isEmpty()) {
            throw new InvalidOperationException(
                    "Reassign this department's employees before moving it to another branch");
        }

        if (departmentRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(
                normalized.branchId(), normalized.name(), id)) {
            throw new DuplicateResourceException(
                    "Branch '" + branch.getName() + "' already has a department named '" + normalized.name() + "'");
        }
        validateHead(normalized.headOfDepartmentId(), normalized.branchId());

        departmentMapper.updateDepartmentFromRequest(department, normalized);
        Department saved = departmentRepository.save(department);
        return toResponseWithNames(saved);
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        return toResponseWithNames(findDepartmentOrThrow(id));
    }

    @Override
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::toResponseWithNames)
                .toList();
    }

    @Override
    public List<DepartmentResponse> getDepartmentsByBranch(Long branchId) {
        return departmentRepository.findByBranchId(branchId).stream()
                .map(this::toResponseWithNames)
                .toList();
    }

    @Override
    public void deleteDepartment(Long id) {
        Department department = findDepartmentOrThrow(id);

        int assigned = employeeRepository.findByDepartmentId(id).size();
        if (assigned > 0) {
            throw new InvalidOperationException(
                    "This department still has " + assigned + " employee(s). Reassign them before deleting it.");
        }

        departmentRepository.delete(department);
    }

    private DepartmentRequest normalize(DepartmentRequest request) {
        return new DepartmentRequest(request.name().trim(), request.branchId(), request.headOfDepartmentId());
    }

    // The head of department must be an employee who works in the department's branch.
    private void validateHead(Long headOfDepartmentId, Long branchId) {
        if (headOfDepartmentId == null) {
            return;
        }
        Employee head = employeeRepository.findById(headOfDepartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + headOfDepartmentId));

        if (!head.getBranchId().equals(branchId)) {
            throw new InvalidOperationException(
                    "The head of department must work in the same branch as the department");
        }
    }

    private Branch findBranchOrThrow(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));
    }

    private Department findDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    private DepartmentResponse toResponseWithNames(Department department) {
        String branchName = branchRepository.findById(department.getBranchId())
                .map(b -> b.getName())
                .orElse(null);

        String headName = department.getHeadOfDepartmentId() != null
                ? employeeRepository.findById(department.getHeadOfDepartmentId())
                .map(e -> e.getFullName())
                .orElse(null)
                : null;

        return departmentMapper.toDepartmentResponse(department, branchName, headName);
    }
}