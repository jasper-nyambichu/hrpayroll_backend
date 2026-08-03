// service/agentadvance/AgentAdvanceService.java
package com.company.hrpayroll.service.agentadvance;

import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRecoveryRequest;
import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRequest;
import com.company.hrpayroll.dto.response.agentadvance.AgentAdvanceResponse;

import java.math.BigDecimal;
import java.util.List;

public interface AgentAdvanceService {
    AgentAdvanceResponse recordAdvance(AgentAdvanceRequest request);
    AgentAdvanceResponse recoverAmount(Long advanceId, AgentAdvanceRecoveryRequest request);
    List<AgentAdvanceResponse> getAdvancesByAgent(Long agentId);

    // Called by payroll/agentformula during a payroll run — not exposed via a controller.
    BigDecimal getRecoverableBalanceForAgent(Long agentId);
    void recoverForPayrollRun(Long agentId, BigDecimal amountToRecover);
}