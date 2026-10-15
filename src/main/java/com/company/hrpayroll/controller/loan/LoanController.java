// controller/loan/LoanController.java
package com.company.hrpayroll.controller.loan;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.loan.LoanApprovalRequest;
import com.company.hrpayroll.dto.request.loan.LoanRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.loan.LoanRepaymentResponse;
import com.company.hrpayroll.dto.response.loan.LoanResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.loan.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final AccessGuard accessGuard;

    // Everyone who may READ company-wide loan data. Finance Officer and Auditor are
    // read-only (proposal §4); approving or rejecting is the Payroll Officer's job.
    private static final String LOAN_READ_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'FINANCE_OFFICER', 'AUDITOR')";

    // An employee requests a loan for themselves; employeeId must match the
    // authenticated principal, to prevent requesting on someone else's behalf.
    // The employee never sets the interest rate — it is dropped here and the
    // approver fixes it on approval.
    @PostMapping("/request")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<LoanResponse> requestMyLoan(@Valid @RequestBody LoanRequest request) {
        if (!request.employeeId().equals(accessGuard.currentEmployeeId())) {
            return ApiResponse.error("You can only request a loan for yourself");
        }
        LoanRequest withoutInterest = new LoanRequest(
                request.employeeId(), request.principalAmount(), null, request.installmentAmount());
        return ApiResponse.success("Loan request submitted", loanService.requestLoan(withoutInterest));
    }

    // Staff entering a loan on an employee's behalf may state the interest up front.
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER')")
    @Auditable(action = "LOAN_REQUESTED", entityType = "Loan")
    public ApiResponse<LoanResponse> createLoanRequest(@Valid @RequestBody LoanRequest request) {
        return ApiResponse.success("Loan request created", loanService.requestLoan(request));
    }

    // Optional body: the approver fixes the flat interest rate for this loan.
    @PutMapping("/{loanId}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PAYROLL_OFFICER')")
    @Auditable(action = "LOAN_APPROVED", entityType = "Loan")
    public ApiResponse<LoanResponse> approveLoan(
            @PathVariable Long loanId,
            @Valid @RequestBody(required = false) LoanApprovalRequest request
    ) {
        BigDecimal interestRate = request != null ? request.interestRate() : null;
        return ApiResponse.success("Loan approved", loanService.approveLoan(loanId, interestRate));
    }

    @PutMapping("/{loanId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PAYROLL_OFFICER')")
    @Auditable(action = "LOAN_REJECTED", entityType = "Loan")
    public ApiResponse<LoanResponse> rejectLoan(@PathVariable Long loanId) {
        return ApiResponse.success("Loan rejected", loanService.rejectLoan(loanId));
    }

    // Company-wide list, newest first. Optional ?status=PENDING|APPROVED|REJECTED|CLOSED.
    @GetMapping
    @PreAuthorize(LOAN_READ_ROLES)
    public ApiResponse<List<LoanResponse>> getAllLoans(@RequestParam(required = false) String status) {
        return ApiResponse.success(loanService.getAllLoans(status));
    }

    @GetMapping("/{loanId}")
    @PreAuthorize(LOAN_READ_ROLES)
    public ApiResponse<LoanResponse> getLoan(@PathVariable Long loanId) {
        return ApiResponse.success(loanService.getLoanById(loanId));
    }

    @GetMapping("/{loanId}/repayments")
    @PreAuthorize(LOAN_READ_ROLES)
    public ApiResponse<List<LoanRepaymentResponse>> getRepayments(@PathVariable Long loanId) {
        return ApiResponse.success(loanService.getRepaymentsForLoan(loanId));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize(LOAN_READ_ROLES + " or @accessGuard.isSameEmployee(#employeeId)")
    public ApiResponse<List<LoanResponse>> getLoansForEmployee(@PathVariable Long employeeId) {
        return ApiResponse.success(loanService.getLoansForEmployee(employeeId));
    }

    @GetMapping("/my-loans")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<List<LoanResponse>> getMyLoans() {
        return ApiResponse.success(loanService.getLoansForEmployee(accessGuard.currentEmployeeId()));
    }
}