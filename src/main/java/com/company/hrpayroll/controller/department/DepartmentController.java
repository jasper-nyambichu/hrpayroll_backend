// controller/department/DepartmentController.java
package com.company.hrpayroll.controller.department;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.department.DepartmentRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.department.DepartmentResponse;
import com.company.hrpayroll.service.department.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Departments are company-wide. The Admin creates them; Branch Managers assign employees to
// them and Agents choose from them when adding staff, so both only need to read the list.
@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private static final String READ_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'AUDITOR', 'BRANCH_MANAGER', 'AGENT', 'BACK_OFFICE')";

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @Auditable(action = "DEPARTMENT_CREATED", entityType = "Department")
    public ApiResponse<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.success("Department created", departmentService.createDepartment(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @Auditable(action = "DEPARTMENT_UPDATED", entityType = "Department")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable Long id, @Valid @RequestBody DepartmentRequest request
    ) {
        return ApiResponse.success("Department updated", departmentService.updateDepartment(id, request));
    }

    // Every department, by name.
    @GetMapping
    @PreAuthorize(READ_ROLES)
    public ApiResponse<List<DepartmentResponse>> getAllDepartments() {
        return ApiResponse.success(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    public ApiResponse<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getDepartmentById(id));
    }

    // Kept for screens written against the old per-branch endpoint: departments are now
    // company-wide, so this returns the same list for every branch.
    @GetMapping("/by-branch/{branchId}")
    @PreAuthorize(READ_ROLES)
    public ApiResponse<List<DepartmentResponse>> getDepartmentsByBranch(@PathVariable Long branchId) {
        return ApiResponse.success(departmentService.getDepartmentsByBranch(branchId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "DEPARTMENT_DELETED", entityType = "Department")
    public ApiResponse<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ApiResponse.success("Department deleted", null);
    }
}