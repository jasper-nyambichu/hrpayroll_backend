// dto/request/payment/PaymentReleaseRequest.java
package com.company.hrpayroll.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// How the Accounts Office sent the money — kept on the request as the proof of release.
public record PaymentReleaseRequest(
        @NotBlank(message = "A release reference is required")
        @Size(min = 3, max = 100, message = "Reference must be between 3 and 100 characters") String reference
) {}
