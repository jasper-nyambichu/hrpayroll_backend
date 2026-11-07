// dto/response/settlement/AgentPaymentReadinessResponse.java
package com.company.hrpayroll.dto.response.settlement;

import java.time.LocalDate;
import java.util.List;

// Whether a period can be calculated, with the days each branch has worked so far, before anything is changed.
public record AgentPaymentReadinessResponse(
        LocalDate periodStart,
        LocalDate periodEnd,
        boolean ready,
        List<BranchReadiness> branches,
        List<String> alreadyPaidAgents
) {
    public record BranchReadiness(Long branchId, String branchName, int employeeCount, int daysWorked) {}
}
