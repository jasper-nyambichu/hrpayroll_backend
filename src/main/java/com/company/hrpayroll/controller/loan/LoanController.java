// controller/loan/LoanController.java
package com.company.hrpayroll.controller.loan;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.loan.LoanRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.loan.LoanResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.loan.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final AccessGuard accessGuard;

    // Same pattern as leave applications — an employee requests a loan for
    // themselves; employeeId comes from the authenticated principal in the
    // ESS flow, not from the request body, to prevent requesting on
    // someone else's behalf. HR/Admin retain the general create path below
    // for cases where staff enter it on an employee's behalf.
    @PostMapping("/request")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<LoanResponse> requestMyLoan(@Valid @RequestBody LoanRequest request) {
        if (!request.employeeId().equals(accessGuard.currentEmployeeId())) {
            return ApiResponse.error("You can only request a loan for yourself");
        }
        return ApiResponse.success("Loan request submitted", loanService.requestLoan(request));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER')")
    @Auditable(action = "LOAN_REQUESTED", entityType = "Loan")
    public ApiResponse<LoanResponse> createLoanRequest(@Valid @RequestBody LoanRequest request) {
        return ApiResponse.success("Loan request created", loanService.requestLoan(request));
    }

    @PutMapping("/{loanId}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PAYROLL_OFFICER', 'FINANCE_OFFICER')")
    @Auditable(action = "LOAN_APPROVED", entityType = "Loan")
    public ApiResponse<LoanResponse> approveLoan(@PathVariable Long loanId) {
        return ApiResponse.success("Loan approved", loanService.approveLoan(loanId));
    }

    @PutMapping("/{loanId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PAYROLL_OFFICER', 'FINANCE_OFFICER')")
    @Auditable(action = "LOAN_REJECTED", entityType = "Loan")
    public ApiResponse<LoanResponse> rejectLoan(@PathVariable Long loanId) {
        return ApiResponse.success("Loan rejected", loanService.rejectLoan(loanId));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'FINANCE_OFFICER') " +
            "or @accessGuard.isSameEmployee(#employeeId)")
    public ApiResponse<List<LoanResponse>> getLoansForEmployee(@PathVariable Long employeeId) {
        return ApiResponse.success(loanService.getLoansForEmployee(employeeId));
    }

    @GetMapping("/my-loans")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<List<LoanResponse>> getMyLoans() {
        return ApiResponse.success(loanService.getLoansForEmployee(accessGuard.currentEmployeeId()));
    }
}