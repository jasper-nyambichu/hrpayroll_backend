// controller/employee/EmployeeController.java
package com.company.hrpayroll.controller.employee;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.employee.EmployeeRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.employee.EmployeeResponse;
import com.company.hrpayroll.dto.response.employee.EmployeeSummaryResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.employee.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final AccessGuard accessGuard;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @Auditable(action = "EMPLOYEE_CREATED", entityType = "Employee")
    public ApiResponse<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.success("Employee created", employeeService.createEmployee(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @Auditable(action = "EMPLOYEE_UPDATED", entityType = "Employee")
    public ApiResponse<EmployeeResponse> updateEmployee(
            @PathVariable Long id, @Valid @RequestBody EmployeeRequest request
    ) {
        return ApiResponse.success("Employee updated", employeeService.updateEmployee(id, request));
    }

    // Fixed: correctly scoped per the gap-fix — Super Admin/Admin/HR/Payroll
    // see any employee; a Branch Manager only if the employee is actually
    // in their branch; an Agent only if the employee is actually theirs;
    // an Employee only their own record.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER') " +
            "or (hasRole('BRANCH_MANAGER') and @accessGuard.isEmployeeInMyBranch(#id)) " +
            "or (hasRole('AGENT') and @accessGuard.isEmployeeOfMyAgent(#id)) " +
            "or @accessGuard.isSameEmployee(#id)")
    public ApiResponse<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ApiResponse.success(employeeService.getEmployeeById(id));
    }

    // The real branch boundary — a Branch Manager can only list employees
    // for their own branch.
    @GetMapping("/by-branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER') or @accessGuard.isSameBranch(#branchId)")
    public ApiResponse<List<EmployeeSummaryResponse>> getEmployeesByBranch(@PathVariable Long branchId) {
        return ApiResponse.success(employeeService.getEmployeesByBranch(branchId));
    }

    // The Agent Portal boundary — an agent only ever sees their own workers.
    @GetMapping("/by-agent/{agentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER') or @accessGuard.isSameAgent(#agentId)")
    public ApiResponse<List<EmployeeSummaryResponse>> getEmployeesByAgent(@PathVariable Long agentId) {
        return ApiResponse.success(employeeService.getEmployeesByAgent(agentId));
    }

    @GetMapping("/by-department/{departmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'BRANCH_MANAGER')")
    public ApiResponse<List<EmployeeSummaryResponse>> getEmployeesByDepartment(@PathVariable Long departmentId) {
        return ApiResponse.success(employeeService.getEmployeesByDepartment(departmentId));
    }

    // ESS convenience endpoint — an employee viewing their own profile.
    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<EmployeeResponse> getMyProfile() {
        return ApiResponse.success(employeeService.getEmployeeById(accessGuard.currentEmployeeId()));
    }

    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @Auditable(action = "EMPLOYEE_TERMINATED", entityType = "Employee")
    public ApiResponse<Void> terminateEmployee(@PathVariable Long id) {
        employeeService.terminateEmployee(id);
        return ApiResponse.success("Employee terminated", null);
    }
}