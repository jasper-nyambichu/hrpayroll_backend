// dto/response/agentadvance/AgentAdvanceResponse.java
package com.company.hrpayroll.dto.response.agentadvance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AgentAdvanceResponse(
        Long id,
        Long agentId,
        String agentName,
        BigDecimal amount,
        LocalDate advanceDate,
        String purpose,
        BigDecimal outstandingBalance,
        String status,
        boolean requestedByAgent,
        String rejectionReason,
        LocalDateTime decidedAt
) {}
