// dto/request/payment/PaymentRequestCreateRequest.java
package com.company.hrpayroll.dto.request.payment;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// The agent comes from the signed-in account, never from the request body.
public record PaymentRequestCreateRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @DecimalMax(value = "10000000", message = "Amount is too large") BigDecimal amount,
        @NotBlank(message = "Say what the money is for")
        @Size(max = 500, message = "Purpose must be at most 500 characters") String purpose
) {}
