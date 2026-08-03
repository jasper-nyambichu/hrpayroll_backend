// dto/request/agentadvance/AgentAdvanceRecoveryRequest.java
package com.company.hrpayroll.dto.request.agentadvance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AgentAdvanceRecoveryRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amountToRecover
) {}