// controller/employee/EmployeeController.java
package com.company.hrpayroll.controller.employee;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.employee.DepartmentAssignmentRequest;
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

    // Admin adds employees directly. Branch Managers and Agents propose them through
    // POST /employee-requests and an Admin approves (see EmployeeCreationRequestController).
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
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

    // Puts an employee in one of the company-wide departments and changes nothing else. A Branch
    // Manager may do this only for employees of their own branch.
    @PutMapping("/{id}/department")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER') " +
            "or (hasRole('BRANCH_MANAGER') and @accessGuard.isEmployeeInMyBranch(#id))")
    @Auditable(action = "EMPLOYEE_DEPARTMENT_ASSIGNED", entityType = "Employee")
    public ApiResponse<EmployeeResponse> assignDepartment(
            @PathVariable Long id, @Valid @RequestBody DepartmentAssignmentRequest request
    ) {
        return ApiResponse.success("Department assigned", employeeService.assignDepartment(id, request.departmentId()));
    }

    // Super Admin/Admin/HR/Payroll see any employee; a Branch Manager only if the employee is
    // in their branch; an Agent only if the employee is theirs; an Employee only their own record.
    // The full record carries national ID and bank details, so the Auditor and Back Office read
    // the list endpoints only.
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER') " +
            "or (hasRole('BRANCH_MANAGER') and @accessGuard.isEmployeeInMyBranch(#id)) " +
            "or (hasRole('AGENT') and @accessGuard.isEmployeeOfMyAgent(#id)) " +
            "or @accessGuard.isSameEmployee(#id)")
    public ApiResponse<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ApiResponse.success(employeeService.getEmployeeById(id));
    }

    // The real branch boundary — a Branch Manager can only list employees for their own branch.
    // The Auditor and Back Office read any branch's list.
    @GetMapping("/by-branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'AUDITOR', 'BACK_OFFICE') or @accessGuard.isSameBranch(#branchId)")
    public ApiResponse<List<EmployeeSummaryResponse>> getEmployeesByBranch(@PathVariable Long branchId) {
        return ApiResponse.success(employeeService.getEmployeesByBranch(branchId));
    }

    // The Agent Portal boundary — an agent only ever sees their own workers.
    @GetMapping("/by-agent/{agentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER', 'AUDITOR') or @accessGuard.isSameAgent(#agentId)")
    public ApiResponse<List<EmployeeSummaryResponse>> getEmployeesByAgent(@PathVariable Long agentId) {
        return ApiResponse.success(employeeService.getEmployeesByAgent(agentId));
    }

    @GetMapping("/by-department/{departmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'BRANCH_MANAGER', 'AUDITOR', 'BACK_OFFICE')")
    public ApiResponse<List<EmployeeSummaryResponse>> getEmployeesByDepartment(@PathVariable Long departmentId) {
        return ApiResponse.success(employeeService.getEmployeesByDepartment(departmentId));
    }

    // ESS convenience endpoint — an employee viewing their own profile.
    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<EmployeeResponse> getMyProfile() {
        return ApiResponse.success(employeeService.getEmployeeById(accessGuard.currentEmployeeId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "EMPLOYEE_DELETED", entityType = "Employee")
    public ApiResponse<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ApiResponse.success("Employee deleted", null);
    }

    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    @Auditable(action = "EMPLOYEE_TERMINATED", entityType = "Employee")
    public ApiResponse<Void> terminateEmployee(@PathVariable Long id) {
        employeeService.terminateEmployee(id);
        return ApiResponse.success("Employee terminated", null);
    }
}