// mapper/settlement/AgentSettlementMapper.java
package com.company.hrpayroll.mapper.settlement;

import com.company.hrpayroll.dto.response.settlement.AgentSettlementResponse;
import com.company.hrpayroll.entity.settlement.AgentSettlement;
import org.springframework.stereotype.Service;

@Service
public class AgentSettlementMapper {

    public AgentSettlementResponse toResponse(AgentSettlement settlement, String agentName) {
        return new AgentSettlementResponse(
                settlement.getId(),
                settlement.getAgentId(),
                agentName,
                settlement.getPeriodStart(),
                settlement.getPeriodEnd(),
                settlement.getEmployeeCount(),
                settlement.getDaysWorkedTotal(),
                settlement.getDaysPay(),
                settlement.getPayeTotal(),
                settlement.getNssfTotal(),
                settlement.getNitaTotal(),
                settlement.getAgentFee(),
                settlement.getVatRateUsed(),
                settlement.getVatAmount(),
                settlement.getLoanTotal(),
                settlement.getDamageTotal(),
                settlement.getAdvanceRecovered(),
                settlement.getAmountPayable(),
                settlement.getPaymentRequestId()
        );
    }
}
