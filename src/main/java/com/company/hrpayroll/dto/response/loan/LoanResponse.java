// dto/response/loan/LoanResponse.java
package com.company.hrpayroll.dto.response.loan;

import java.math.BigDecimal;
import java.time.LocalDate;

// totalRepayable = principal + flat interest (principal x interestRate%).
public record LoanResponse(
        Long id,
        Long employeeId,
        String employeeName,
        BigDecimal principalAmount,
        BigDecimal interestRate,
        BigDecimal totalRepayable,
        BigDecimal installmentAmount,
        BigDecimal outstandingBalance,
        LocalDate requestDate,
        LocalDate approvalDate,
        String status
) {}