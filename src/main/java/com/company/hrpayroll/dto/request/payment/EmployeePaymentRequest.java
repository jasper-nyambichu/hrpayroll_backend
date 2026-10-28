// dto/request/payment/EmployeePaymentRequest.java
package com.company.hrpayroll.dto.request.payment;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// paidOn is optional: when omitted the payment is dated today (company time).
public record EmployeePaymentRequest(
        @NotNull(message = "Choose the payment request this is paid from") Long paymentRequestId,
        @NotNull(message = "Employee is required") Long employeeId,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @DecimalMax(value = "10000000", message = "Amount is too large") BigDecimal amount,
        @NotBlank(message = "The M-Pesa transaction code is required")
        @Pattern(regexp = "^[A-Za-z0-9]{10}$", message = "An M-Pesa code is 10 letters and numbers, e.g. QGH7K2L9XP") String mpesaCode,
        @PastOrPresent(message = "Payment date cannot be in the future") LocalDate paidOn,
        @Size(max = 255, message = "Note must be at most 255 characters") String note
) {}
