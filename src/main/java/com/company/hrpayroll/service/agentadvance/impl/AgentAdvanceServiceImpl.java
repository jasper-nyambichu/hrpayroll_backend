// service/agentadvance/impl/AgentAdvanceServiceImpl.java
package com.company.hrpayroll.service.agentadvance.impl;

import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRecoveryRequest;
import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRequest;
import com.company.hrpayroll.dto.response.agentadvance.AgentAdvanceResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.agentadvance.AgentAdvance;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.agentadvance.AgentAdvanceMapper;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.agentadvance.AgentAdvanceRepository;
import com.company.hrpayroll.service.agentadvance.AgentAdvanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentAdvanceServiceImpl implements AgentAdvanceService {

    private final AgentAdvanceRepository agentAdvanceRepository;
    private final AgentRepository agentRepository;
    private final AgentAdvanceMapper agentAdvanceMapper;

    @Override
    public AgentAdvanceResponse recordAdvance(AgentAdvanceRequest request) {
        Agent agent = agentRepository.findById(request.agentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + request.agentId()));

        // Money must not go out to a deactivated agent.
        if ("INACTIVE".equals(agent.getStatus())) {
            throw new InvalidOperationException("Cannot record an advance for an inactive agent");
        }

        AgentAdvance advance = agentAdvanceMapper.toAgentAdvance(request);
        AgentAdvance saved = agentAdvanceRepository.save(advance);
        return toResponseWithAgentName(saved);
    }

    @Override
    public AgentAdvanceResponse recoverAmount(Long advanceId, AgentAdvanceRecoveryRequest request) {
        AgentAdvance advance = findAdvanceOrThrow(advanceId);
        applyRecovery(advance, request.amountToRecover());
        AgentAdvance saved = agentAdvanceRepository.save(advance);
        return toResponseWithAgentName(saved);
    }

    @Override
    public List<AgentAdvanceResponse> getAdvancesByAgent(Long agentId) {
        return agentAdvanceRepository.findByAgentId(agentId).stream()
                .map(this::toResponseWithAgentName)
                .toList();
    }

    @Override
    public List<AgentAdvanceResponse> getAllAdvances() {
        return agentAdvanceRepository.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::toResponseWithAgentName)
                .toList();
    }

    @Override
    public AgentAdvanceResponse getAdvanceById(Long advanceId) {
        return toResponseWithAgentName(findAdvanceOrThrow(advanceId));
    }

    @Override
    public BigDecimal getRecoverableBalanceForAgent(Long agentId) {
        return agentAdvanceRepository.findRecoverableByAgentId(agentId).stream()
                .map(AgentAdvance::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public void recoverForPayrollRun(Long agentId, BigDecimal amountToRecover) {
        // Recovers from the oldest outstanding advances first, spilling into
        // the next one if a single advance doesn't cover the full amount.
        BigDecimal remaining = amountToRecover;

        List<AgentAdvance> recoverable = agentAdvanceRepository.findRecoverableByAgentId(agentId);
        for (AgentAdvance advance : recoverable) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal amountFromThisAdvance = remaining.min(advance.getOutstandingBalance());
            applyRecovery(advance, amountFromThisAdvance);
            agentAdvanceRepository.save(advance);
            remaining = remaining.subtract(amountFromThisAdvance);
        }
    }

    private void applyRecovery(AgentAdvance advance, BigDecimal amountToRecover) {
        if (amountToRecover.compareTo(advance.getOutstandingBalance()) > 0) {
            throw new InvalidOperationException(
                    "Cannot recover " + amountToRecover + " — only " +
                            advance.getOutstandingBalance() + " is outstanding on this advance");
        }

        BigDecimal newBalance = advance.getOutstandingBalance().subtract(amountToRecover);
        advance.setOutstandingBalance(newBalance);
        advance.setStatus(newBalance.compareTo(BigDecimal.ZERO) == 0 ? "FULLY_RECOVERED" : "PARTIALLY_RECOVERED");
    }

    private AgentAdvance findAdvanceOrThrow(Long id) {
        return agentAdvanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent advance not found with id: " + id));
    }

    private AgentAdvanceResponse toResponseWithAgentName(AgentAdvance advance) {
        String agentName = agentRepository.findById(advance.getAgentId())
                .map(Agent::getName)
                .orElse(null);
        return agentAdvanceMapper.toAgentAdvanceResponse(advance, agentName);
    }
}