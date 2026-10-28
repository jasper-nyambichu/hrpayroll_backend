// service/agentadvance/AgentAdvanceService.java
package com.company.hrpayroll.service.agentadvance;

import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRecoveryRequest;
import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRequest;
import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceSubmitRequest;
import com.company.hrpayroll.dto.response.agentadvance.AgentAdvanceResponse;

import java.math.BigDecimal;
import java.util.List;

public interface AgentAdvanceService {
    AgentAdvanceResponse recordAdvance(AgentAdvanceRequest request);

    // An agent asks for an advance. It stays PENDING (and is never recovered) until the Admin decides;
    // every Admin is told.
    AgentAdvanceResponse requestAdvance(AgentAdvanceSubmitRequest request, Long agentId);
    AgentAdvanceResponse approveAdvance(Long advanceId, Long adminUserId);
    AgentAdvanceResponse rejectAdvance(Long advanceId, String reason, Long adminUserId);
    AgentAdvanceResponse recoverAmount(Long advanceId, AgentAdvanceRecoveryRequest request);
    List<AgentAdvanceResponse> getAdvancesByAgent(Long agentId);

    // Company-wide list (newest first) and single record, for Payroll / Finance / Audit screens.
    List<AgentAdvanceResponse> getAllAdvances();
    AgentAdvanceResponse getAdvanceById(Long advanceId);

    // Called by payroll/agentformula during a payroll run — not exposed via a controller.
    BigDecimal getRecoverableBalanceForAgent(Long agentId);
    void recoverForPayrollRun(Long agentId, BigDecimal amountToRecover);
}