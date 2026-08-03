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

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getDepartmentById(id));
    }

    // Branch-scoped by nature of the query itself — a Branch Manager calling
    // this with their own branchId is safe; calling it with someone else's
    // is blocked by AccessGuard the same way BranchController blocks it.
    @GetMapping("/by-branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER') or @accessGuard.isSameBranch(#branchId)")
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