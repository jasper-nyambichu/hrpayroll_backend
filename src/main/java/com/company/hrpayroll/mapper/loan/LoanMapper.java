// mapper/loan/LoanMapper.java
package com.company.hrpayroll.mapper.loan;

import com.company.hrpayroll.dto.request.loan.LoanRequest;
import com.company.hrpayroll.dto.response.loan.LoanRepaymentResponse;
import com.company.hrpayroll.dto.response.loan.LoanResponse;
import com.company.hrpayroll.entity.loan.Loan;
import com.company.hrpayroll.entity.loan.LoanRepayment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class LoanMapper {

    public Loan toLoan(LoanRequest dto) {
        BigDecimal interestRate = dto.interestRate() != null ? dto.interestRate() : BigDecimal.ZERO;

        return Loan.builder()
                .employeeId(dto.employeeId())
                .principalAmount(dto.principalAmount())
                .interestRate(interestRate)
                .installmentAmount(dto.installmentAmount())
                // A loan is created owing the FULL amount: principal plus interest.
                .outstandingBalance(totalRepayable(dto.principalAmount(), interestRate))
                .status("PENDING")
                .build();
    }

    public LoanResponse toLoanResponse(Loan loan, String employeeName) {
        return new LoanResponse(
                loan.getId(),
                loan.getEmployeeId(),
                employeeName,
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                totalRepayable(loan.getPrincipalAmount(), loan.getInterestRate()),
                loan.getInstallmentAmount(),
                loan.getOutstandingBalance(),
                loan.getRequestDate(),
                loan.getApprovalDate(),
                loan.getStatus()
        );
    }

    public LoanRepaymentResponse toLoanRepaymentResponse(LoanRepayment repayment) {
        return new LoanRepaymentResponse(
                repayment.getId(),
                repayment.getLoanId(),
                repayment.getPayrollRunId(),
                repayment.getAmountPaid(),
                repayment.getCreatedAt()
        );
    }

    // Flat interest: principal + principal x rate%. A missing rate means no interest.
    // This is the single place that defines how interest works — change it here if
    // the company later prefers a per-annum or reducing-balance rule.
    public static BigDecimal totalRepayable(BigDecimal principal, BigDecimal interestRate) {
        BigDecimal rate = interestRate != null ? interestRate : BigDecimal.ZERO;
        BigDecimal interest = principal.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return principal.add(interest).setScale(2, RoundingMode.HALF_UP);
    }
}