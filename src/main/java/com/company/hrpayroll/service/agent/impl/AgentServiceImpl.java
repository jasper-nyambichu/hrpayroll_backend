// service/agent/impl/AgentServiceImpl.java
package com.company.hrpayroll.service.agent.impl;

import com.company.hrpayroll.dto.request.agent.AgentRequest;
import com.company.hrpayroll.dto.response.agent.AgentResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.agent.ChargeType;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.agent.AgentMapper;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.agent.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;
    private final EmployeeRepository employeeRepository;
    private final AgentMapper agentMapper;

    @Override
    public AgentResponse createAgent(AgentRequest request) {
        validateChargeConfiguration(request);

        Agent agent = agentMapper.toAgent(request);
        Agent saved = agentRepository.save(agent);
        return toResponseWithEmployeeCount(saved);
    }

    @Override
    public AgentResponse updateAgent(Long id, AgentRequest request) {
        validateChargeConfiguration(request);

        Agent agent = findAgentOrThrow(id);
        agentMapper.updateAgentFromRequest(agent, request);
        Agent saved = agentRepository.save(agent);
        return toResponseWithEmployeeCount(saved);
    }

    @Override
    public AgentResponse getAgentById(Long id) {
        return toResponseWithEmployeeCount(findAgentOrThrow(id));
    }

    @Override
    public List<AgentResponse> getAllAgents() {
        return agentRepository.findAll().stream()
                .map(this::toResponseWithEmployeeCount)
                .toList();
    }

    @Override
    public List<AgentResponse> getAgentsByIds(List<Long> ids) {
        return agentRepository.findByIdIn(ids).stream()
                .map(this::toResponseWithEmployeeCount)
                .toList();
    }

    @Override
    public void deactivateAgent(Long id) {
        Agent agent = findAgentOrThrow(id);
        agent.setStatus("INACTIVE");
        agentRepository.save(agent);
    }

    // The charge type determines which amount field is actually required —
    // this is exactly the kind of rule a DTO annotation can't express, so it
    // lives here in the service instead.
    private void validateChargeConfiguration(AgentRequest request) {
        ChargeType type = request.chargeType();

        if (type == ChargeType.PERCENTAGE_OF_GROSS) {
            if (request.chargePercentage() == null) {
                throw new InvalidOperationException(
                        "Charge percentage is required when charge type is PERCENTAGE_OF_GROSS");
            }
        } else {
            if (request.chargeAmount() == null) {
                throw new InvalidOperationException(
                        "Charge amount is required for charge type " + type);
            }
        }
    }

    private Agent findAgentOrThrow(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + id));
    }

    private AgentResponse toResponseWithEmployeeCount(Agent agent) {
        long count = employeeRepository.countByAgentIdAndEmploymentStatus(agent.getId(), "ACTIVE");
        return agentMapper.toAgentResponse(agent, (int) count);
    }
}