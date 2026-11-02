// controller/employee/EmployeeController.java
package com.company.hrpayroll.controller.employee;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.employee.DepartmentAssignmentRequest;
import com.company.hrpayroll.dto.request.employee.EmployeeRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.employee.EmployeeDirectoryResponse;
import com.company.hrpayroll.dto.response.employee.EmployeeResponse;
import com.company.hrpayroll.dto.response.employee.EmployeeSummaryResponse;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.employee.EmployeeExportService;
import com.company.hrpayroll.service.employee.EmployeeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeExportService employeeExportService;
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
        return ApiResponse.success("Department assigned", forViewer(employeeService.assignDepartment(id, request.departmentId())));
    }

    private EmployeeResponse forViewer(EmployeeResponse employee) {
        return accessGuard.isBranchManager() ? employee.withoutSalary() : employee;
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
        return ApiResponse.success(forViewer(employeeService.getEmployeeById(id)));
    }

    // The real branch boundary — a Branch Manager can only list employees for their own branch.
    // The Auditor and Back Office read any branch's list.
    @GetMapping("/by-branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'AUDITOR', 'BACK_OFFICE') or @accessGuard.isSameBranch(#branchId)")
    public ApiResponse<List<EmployeeSummaryResponse>> getEmployeesByBranch(@PathVariable Long branchId) {
        return ApiResponse.success(employeeService.getEmployeesByBranch(branchId));
    }

    // Downloads the employee list as a PDF (with the company logo) or CSV. Who is signed in decides
    // what it holds, whatever the request asks for: an Admin any branch or all of them, a Branch
    // Manager their own branch, an Agent only their own employees. Branch Managers do not get salary.
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BRANCH_MANAGER', 'AGENT')")
    public ResponseEntity<byte[]> exportEmployees(
            @RequestParam(defaultValue = "PDF") EmployeeExportService.Format format,
            @RequestParam(required = false) Long branchId
    ) {
        GeneratedDocument document;
        if (accessGuard.isAgent()) {
            if (accessGuard.currentAgentId() == null) {
                throw new InvalidOperationException("Your account is not linked to an agent record.");
            }
            document = employeeExportService.export(format, branchId, accessGuard.currentAgentId(), true);
        } else if (accessGuard.isBranchManager()) {
            if (accessGuard.currentBranchId() == null) {
                throw new InvalidOperationException("Your account is not assigned to a branch. Ask an Admin to assign you one.");
            }
            document = employeeExportService.export(format, accessGuard.currentBranchId(), null, false);
        } else {
            document = employeeExportService.export(format, branchId, null, true);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", document.fileName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(document.contentType()))
                .body(document.content());
    }

    // A branch's list, with basic salary. Admin only.
    @GetMapping("/directory/by-branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<List<EmployeeDirectoryResponse>> getBranchDirectory(@PathVariable Long branchId) {
        return ApiResponse.success(employeeService.getBranchDirectory(branchId));
    }

    // The same list for a Branch Manager: their own branch, without salary.
    @GetMapping("/my-branch-directory")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ApiResponse<List<EmployeeDirectoryResponse>> getMyBranchEmployees() {
        if (accessGuard.currentBranchId() == null) {
            throw new InvalidOperationException("Your account is not assigned to a branch. Ask an Admin to assign you one.");
        }
        return ApiResponse.success(employeeService.getBranchDirectory(accessGuard.currentBranchId()).stream()
                .map(EmployeeDirectoryResponse::withoutSalary)
                .toList());
    }

    // The same list for an agent: only their own employees.
    @GetMapping("/my-directory/by-branch/{branchId}")
    @PreAuthorize("hasRole('AGENT')")
    public ApiResponse<List<EmployeeDirectoryResponse>> getMyBranchDirectory(@PathVariable Long branchId) {
        return ApiResponse.success(employeeService.getBranchDirectoryForAgent(branchId, accessGuard.currentAgentId()));
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

    public record EmployeeBranchRequest(@NotNull(message = "Branch is required") Long branchId) {}

    @PutMapping("/{id}/branch")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "EMPLOYEE_BRANCH_CHANGED", entityType = "Employee")
    public ApiResponse<EmployeeResponse> assignBranch(
            @PathVariable Long id, @Valid @RequestBody EmployeeBranchRequest request
    ) {
        return ApiResponse.success("Branch changed", employeeService.assignBranch(id, request.branchId()));
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