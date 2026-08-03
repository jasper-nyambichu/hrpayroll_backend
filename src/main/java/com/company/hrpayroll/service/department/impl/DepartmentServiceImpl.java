// service/department/impl/DepartmentServiceImpl.java
package com.company.hrpayroll.service.department.impl;

import com.company.hrpayroll.dto.request.department.DepartmentRequest;
import com.company.hrpayroll.dto.response.department.DepartmentResponse;
import com.company.hrpayroll.entity.department.Department;
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
        // A department must belong to a branch that actually exists.
        if (!branchRepository.existsById(request.branchId())) {
            throw new ResourceNotFoundException("Branch not found with id: " + request.branchId());
        }

        Department department = departmentMapper.toDepartment(request);
        Department saved = departmentRepository.save(department);
        return toResponseWithNames(saved);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = findDepartmentOrThrow(id);

        if (!branchRepository.existsById(request.branchId())) {
            throw new ResourceNotFoundException("Branch not found with id: " + request.branchId());
        }

        departmentMapper.updateDepartmentFromRequest(department, request);
        Department saved = departmentRepository.save(department);
        return toResponseWithNames(saved);
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        return toResponseWithNames(findDepartmentOrThrow(id));
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
        departmentRepository.delete(department);
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