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
    /** What a payment run would take back from each advance, oldest first. Changes nothing. */
    List<AdvanceRecovery> planRecovery(Long agentId, BigDecimal amountToRecover);

    /** Takes it back (oldest advance first) and says how much came from each advance. */
    List<AdvanceRecovery> recoverForPayrollRun(Long agentId, BigDecimal amountToRecover);

    /** One advance's share of a recovery, and what is still owed on it afterwards. */
    record AdvanceRecovery(Long advanceId, BigDecimal amount, BigDecimal balanceAfter) {}
}