// dto/request/payment/PaymentRejectionRequest.java
package com.company.hrpayroll.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// A rejection always says why, so the agent knows what to change.
public record PaymentRejectionRequest(
        @NotBlank(message = "A reason is required")
        @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters") String reason
) {}
