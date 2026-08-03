// dto/request/agentadvance/AgentAdvanceRequest.java
package com.company.hrpayroll.dto.request.agentadvance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AgentAdvanceRequest(
        @NotNull(message = "Agent is required") Long agentId,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
        @NotNull(message = "Advance date is required") LocalDate advanceDate,
        String purpose
) {}