// dto/request/loan/LoanRequest.java
package com.company.hrpayroll.dto.request.loan;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoanRequest(
        @NotNull(message = "Employee is required") Long employeeId,
        @NotNull(message = "Principal amount is required")
        @DecimalMin(value = "0.01", message = "Principal amount must be greater than zero") BigDecimal principalAmount,
        @DecimalMin(value = "0.00", message = "Interest rate cannot be negative")
        @DecimalMax(value = "100.00", message = "Interest rate cannot exceed 100") BigDecimal interestRate,
        @NotNull(message = "Installment amount is required")
        @DecimalMin(value = "0.01", message = "Installment amount must be greater than zero") BigDecimal installmentAmount
) {}