// service/agent/AgentService.java
package com.company.hrpayroll.service.agent;

import com.company.hrpayroll.dto.request.agent.AgentRequest;
import com.company.hrpayroll.dto.response.agent.AgentResponse;

import java.util.List;

public interface AgentService {
    AgentResponse createAgent(AgentRequest request);
    AgentResponse updateAgent(Long id, AgentRequest request);
    AgentResponse getAgentById(Long id);
    List<AgentResponse> getAllAgents();
    List<AgentResponse> getAgentsByIds(List<Long> ids);
    void deactivateAgent(Long id);
}