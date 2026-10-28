// service/loan/LoanService.java
package com.company.hrpayroll.service.loan;

import com.company.hrpayroll.dto.request.loan.LoanRequest;
import com.company.hrpayroll.dto.response.loan.LoanRepaymentResponse;
import com.company.hrpayroll.dto.response.loan.LoanResponse;

import java.math.BigDecimal;
import java.util.List;

public interface LoanService {
    LoanResponse requestLoan(LoanRequest request);

    // An agent asks for a loan for one of THEIR employees (never one they do not supply). The interest is
    // left for the approver to fix; every Admin is told there is a request to decide.
    LoanResponse requestLoanForAgent(LoanRequest request, Long agentId);

    // interestRate is optional: when given, the approver is fixing the flat interest
    // for this loan and the balance becomes principal + interest.
    LoanResponse approveLoan(Long loanId, BigDecimal interestRate);
    LoanResponse rejectLoan(Long loanId);
    List<LoanResponse> getLoansForEmployee(Long employeeId);

    // The loans of every employee the agent supplies, newest first.
    List<LoanResponse> getLoansForAgent(Long agentId);

    // Company-wide list for Payroll / Finance / Audit screens; status is optional
    // (PENDING, APPROVED, REJECTED, CLOSED). Newest first.
    List<LoanResponse> getAllLoans(String status);
    LoanResponse getLoanById(Long loanId);
    List<LoanRepaymentResponse> getRepaymentsForLoan(Long loanId);

    // Called by payroll/engine during a run — not exposed via a controller.
    List<LoanDeductionResult> getActiveDeductionsForEmployees(List<Long> employeeIds);
    void applyDeduction(Long loanId, Long payrollRunId, BigDecimal amountPaid);

    record LoanDeductionResult(Long loanId, Long employeeId, BigDecimal installmentAmount) {}
}