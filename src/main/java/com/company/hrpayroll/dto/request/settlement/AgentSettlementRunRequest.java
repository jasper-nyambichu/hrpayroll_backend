// dto/request/settlement/AgentSettlementRunRequest.java
package com.company.hrpayroll.dto.request.settlement;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

// agentIds is optional: leaving it out calculates every active agent with active employees.
public record AgentSettlementRunRequest(
        @NotNull(message = "Period start is required") LocalDate periodStart,
        @NotNull(message = "Period end is required") LocalDate periodEnd,
        List<Long> agentIds
) {}
