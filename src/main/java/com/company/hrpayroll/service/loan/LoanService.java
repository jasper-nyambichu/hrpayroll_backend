// service/loan/LoanService.java
package com.company.hrpayroll.service.loan;

import com.company.hrpayroll.dto.request.loan.LoanRequest;
import com.company.hrpayroll.dto.response.loan.LoanResponse;

import java.math.BigDecimal;
import java.util.List;

public interface LoanService {
    LoanResponse requestLoan(LoanRequest request);
    LoanResponse approveLoan(Long loanId);
    LoanResponse rejectLoan(Long loanId);
    List<LoanResponse> getLoansForEmployee(Long employeeId);

    // Called by payroll/engine during a run — not exposed via a controller.
    List<LoanDeductionResult> getActiveDeductionsForEmployees(List<Long> employeeIds);
    void applyDeduction(Long loanId, Long payrollRunId, BigDecimal amountPaid);

    record LoanDeductionResult(Long loanId, Long employeeId, BigDecimal installmentAmount) {}
}