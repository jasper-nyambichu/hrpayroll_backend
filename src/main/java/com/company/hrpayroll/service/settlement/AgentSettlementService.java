// service/settlement/AgentSettlementService.java
package com.company.hrpayroll.service.settlement;

import com.company.hrpayroll.dto.request.settlement.AgentSettlementRunRequest;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.settlement.AgentPaymentReadinessResponse;
import com.company.hrpayroll.dto.response.settlement.AgentSettlementResponse;

import java.time.LocalDate;
import java.util.List;

public interface AgentSettlementService {

    // Admin: works out each agent's amount for the period, takes the loans, damages and advances it
    // used, and creates an approved payment request for the Admin to release. All or nothing.
    List<AgentSettlementResponse> calculate(AgentSettlementRunRequest request, Long adminUserId);

    AgentPaymentReadinessResponse getReadiness(LocalDate periodStart, LocalDate periodEnd);

    List<AgentSettlementResponse> getAll();
    List<AgentSettlementResponse> getForAgent(Long agentId);
    AgentSettlementResponse getById(Long settlementId);

    GeneratedDocument getInvoice(Long settlementId);
    GeneratedDocument getStatement(Long settlementId);
}
