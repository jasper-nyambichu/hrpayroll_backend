// dto/request/loan/LoanApprovalRequest.java
package com.company.hrpayroll.dto.request.loan;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

// Optional body on approval: the approver fixes the (flat) interest rate for the loan.
// Omitted = keep whatever rate the loan already has (0 for employee requests).
public record LoanApprovalRequest(
        @DecimalMin(value = "0.00", message = "Interest rate cannot be negative")
        @DecimalMax(value = "100.00", message = "Interest rate cannot exceed 100") BigDecimal interestRate
) {}