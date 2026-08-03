// mapper/agentadvance/AgentAdvanceMapper.java
package com.company.hrpayroll.mapper.agentadvance;

import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRequest;
import com.company.hrpayroll.dto.response.agentadvance.AgentAdvanceResponse;
import com.company.hrpayroll.entity.agentadvance.AgentAdvance;
import org.springframework.stereotype.Service;

@Service
public class AgentAdvanceMapper {

    public AgentAdvance toAgentAdvance(AgentAdvanceRequest dto) {
        return AgentAdvance.builder()
                .agentId(dto.agentId())
                .amount(dto.amount())
                .advanceDate(dto.advanceDate())
                .purpose(dto.purpose())
                .outstandingBalance(dto.amount()) // fully outstanding at creation
                .status("OUTSTANDING")
                .build();
    }

    public AgentAdvanceResponse toAgentAdvanceResponse(AgentAdvance advance, String agentName) {
        return new AgentAdvanceResponse(
                advance.getId(),
                advance.getAgentId(),
                agentName,
                advance.getAmount(),
                advance.getAdvanceDate(),
                advance.getPurpose(),
                advance.getOutstandingBalance(),
                advance.getStatus()
        );
    }
}