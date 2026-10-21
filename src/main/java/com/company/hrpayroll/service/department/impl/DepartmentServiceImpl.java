// service/department/impl/DepartmentServiceImpl.java
package com.company.hrpayroll.service.department.impl;

import com.company.hrpayroll.dto.request.department.DepartmentRequest;
import com.company.hrpayroll.dto.response.department.DepartmentResponse;
import com.company.hrpayroll.entity.department.Department;
import com.company.hrpayroll.exception.DuplicateResourceException;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.department.DepartmentMapper;
import com.company.hrpayroll.repository.department.DepartmentRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.department.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Departments are company-wide: one list (Finance, Operations, …) that every branch uses, so an
 * employee in any branch is assigned from the same set. A department's head is any employee.
 */
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        DepartmentRequest normalized = normalize(request);

        if (departmentRepository.existsByNameIgnoreCase(normalized.name())) {
            throw new DuplicateResourceException("A department named '" + normalized.name() + "' already exists");
        }
        validateHead(normalized.headOfDepartmentId());

        Department saved = departmentRepository.save(departmentMapper.toDepartment(normalized));
        return toResponseWithNames(saved);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = findDepartmentOrThrow(id);
        DepartmentRequest normalized = normalize(request);

        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(normalized.name(), id)) {
            throw new DuplicateResourceException("A department named '" + normalized.name() + "' already exists");
        }
        validateHead(normalized.headOfDepartmentId());

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
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponseWithNames)
                .toList();
    }

    // Departments are no longer per branch, so every branch sees the same list. Kept so
    // screens written against the old per-branch endpoint keep working.
    @Override
    public List<DepartmentResponse> getDepartmentsByBranch(Long branchId) {
        return getAllDepartments();
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

    // Name only — the company-wide list has no branch. Any branchId sent is discarded.
    private DepartmentRequest normalize(DepartmentRequest request) {
        return new DepartmentRequest(request.name().trim(), null, request.headOfDepartmentId());
    }

    private void validateHead(Long headOfDepartmentId) {
        if (headOfDepartmentId != null && !employeeRepository.existsById(headOfDepartmentId)) {
            throw new ResourceNotFoundException("Employee not found with id: " + headOfDepartmentId);
        }
    }

    private Department findDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    private DepartmentResponse toResponseWithNames(Department department) {
        String headName = department.getHeadOfDepartmentId() != null
                ? employeeRepository.findById(department.getHeadOfDepartmentId())
                .map(e -> e.getFullName())
                .orElse(null)
                : null;

        return departmentMapper.toDepartmentResponse(department, null, headName);
    }
}