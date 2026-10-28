// dto/request/agentadvance/AgentAdvanceRejectionRequest.java
package com.company.hrpayroll.dto.request.agentadvance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentAdvanceRejectionRequest(
        @NotBlank(message = "A reason is required")
        @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters") String reason
) {}
