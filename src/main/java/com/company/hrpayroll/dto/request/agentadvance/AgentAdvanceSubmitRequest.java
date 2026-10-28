// dto/request/agentadvance/AgentAdvanceSubmitRequest.java
package com.company.hrpayroll.dto.request.agentadvance;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// An agent asking for an advance. The agent comes from the signed-in account, and the advance date
// is the day the Admin approves it, so neither is in the body.
public record AgentAdvanceSubmitRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @DecimalMax(value = "10000000", message = "Amount is too large") BigDecimal amount,
        @NotBlank(message = "Say what the advance is for")
        @Size(max = 255, message = "Purpose must be at most 255 characters") String purpose
) {}
