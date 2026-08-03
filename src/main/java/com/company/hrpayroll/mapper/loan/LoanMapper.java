// mapper/loan/LoanMapper.java
package com.company.hrpayroll.mapper.loan;

import com.company.hrpayroll.dto.request.loan.LoanRequest;
import com.company.hrpayroll.dto.response.loan.LoanResponse;
import com.company.hrpayroll.entity.loan.Loan;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class LoanMapper {

    public Loan toLoan(LoanRequest dto) {
        return Loan.builder()
                .employeeId(dto.employeeId())
                .principalAmount(dto.principalAmount())
                .interestRate(dto.interestRate() != null ? dto.interestRate() : BigDecimal.ZERO)
                .installmentAmount(dto.installmentAmount())
                .outstandingBalance(dto.principalAmount())
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
                loan.getInstallmentAmount(),
                loan.getOutstandingBalance(),
                loan.getRequestDate(),
                loan.getApprovalDate(),
                loan.getStatus()
        );
    }
}