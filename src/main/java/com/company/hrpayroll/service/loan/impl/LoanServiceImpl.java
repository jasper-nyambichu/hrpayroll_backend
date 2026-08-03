// service/loan/impl/LoanServiceImpl.java
package com.company.hrpayroll.service.loan.impl;

import com.company.hrpayroll.dto.request.loan.LoanRequest;
import com.company.hrpayroll.dto.response.loan.LoanResponse;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.loan.Loan;
import com.company.hrpayroll.entity.loan.LoanRepayment;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.loan.LoanMapper;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.loan.LoanRepaymentRepository;
import com.company.hrpayroll.repository.loan.LoanRepository;
import com.company.hrpayroll.service.loan.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final EmployeeRepository employeeRepository;
    private final LoanMapper loanMapper;

    @Override
    public LoanResponse requestLoan(LoanRequest request) {
        if (!employeeRepository.existsById(request.employeeId())) {
            throw new ResourceNotFoundException("Employee not found with id: " + request.employeeId());
        }

        Loan loan = loanMapper.toLoan(request);
        loan.setRequestDate(LocalDate.now());
        Loan saved = loanRepository.save(loan);
        return toResponseWithEmployeeName(saved);
    }

    @Override
    public LoanResponse approveLoan(Long loanId) {
        Loan loan = findOrThrow(loanId);
        if (!"PENDING".equals(loan.getStatus())) {
            throw new InvalidOperationException("Only pending loans can be approved");
        }
        loan.setStatus("APPROVED");
        loan.setApprovalDate(LocalDate.now());
        Loan saved = loanRepository.save(loan);
        return toResponseWithEmployeeName(saved);
    }

    @Override
    public LoanResponse rejectLoan(Long loanId) {
        Loan loan = findOrThrow(loanId);
        if (!"PENDING".equals(loan.getStatus())) {
            throw new InvalidOperationException("Only pending loans can be rejected");
        }
        loan.setStatus("REJECTED");
        Loan saved = loanRepository.save(loan);
        return toResponseWithEmployeeName(saved);
    }

    @Override
    public List<LoanResponse> getLoansForEmployee(Long employeeId) {
        return loanRepository.findByEmployeeId(employeeId).stream()
                .map(this::toResponseWithEmployeeName)
                .toList();
    }

    @Override
    public List<LoanDeductionResult> getActiveDeductionsForEmployees(List<Long> employeeIds) {
        return loanRepository.findByEmployeeIdInAndStatus(employeeIds, "APPROVED").stream()
                // Never deduct more than what's actually still owed.
                .map(loan -> new LoanDeductionResult(
                        loan.getId(),
                        loan.getEmployeeId(),
                        loan.getInstallmentAmount().min(loan.getOutstandingBalance())
                ))
                .filter(result -> result.installmentAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    @Override
    public void applyDeduction(Long loanId, Long payrollRunId, BigDecimal amountPaid) {
        Loan loan = findOrThrow(loanId);

        BigDecimal newBalance = loan.getOutstandingBalance().subtract(amountPaid);
        loan.setOutstandingBalance(newBalance.max(BigDecimal.ZERO));
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus("CLOSED");
        }
        loanRepository.save(loan);

        LoanRepayment repayment = LoanRepayment.builder()
                .loanId(loanId)
                .payrollRunId(payrollRunId)
                .amountPaid(amountPaid)
                .build();
        loanRepaymentRepository.save(repayment);
    }

    private Loan findOrThrow(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + id));
    }

    private LoanResponse toResponseWithEmployeeName(Loan loan) {
        String employeeName = employeeRepository.findById(loan.getEmployeeId())
                .map(Employee::getFullName).orElse(null);
        return loanMapper.toLoanResponse(loan, employeeName);
    }
}