// dto/response/loan/LoanRepaymentResponse.java
package com.company.hrpayroll.dto.response.loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// One instalment taken from payroll. paidOn is when the payroll run recorded it.
public record LoanRepaymentResponse(
        Long id,
        Long loanId,
        Long payrollRunId,
        BigDecimal amountPaid,
        LocalDateTime paidOn
) {}