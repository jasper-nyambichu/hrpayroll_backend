// service/agentadvance/impl/AgentAdvanceServiceImpl.java
package com.company.hrpayroll.service.agentadvance.impl;

import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRecoveryRequest;
import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRequest;
import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceSubmitRequest;
import com.company.hrpayroll.dto.response.agentadvance.AgentAdvanceResponse;
import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.agentadvance.AgentAdvance;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.agentadvance.AgentAdvanceMapper;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.agentadvance.AgentAdvanceRepository;
import com.company.hrpayroll.service.agentadvance.AgentAdvanceService;
import com.company.hrpayroll.service.notification.NotificationRecipients;
import com.company.hrpayroll.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AgentAdvanceServiceImpl implements AgentAdvanceService {

    private final AgentAdvanceRepository agentAdvanceRepository;
    private final AgentRepository agentRepository;
    private final AgentAdvanceMapper agentAdvanceMapper;
    private final BusinessClock businessClock;
    private final NotificationService notificationService;
    private final NotificationRecipients recipients;

    private static final String TYPE_REQUESTED = "AGENT_ADVANCE_REQUESTED";
    private static final String TYPE_APPROVED = "AGENT_ADVANCE_APPROVED";
    private static final String TYPE_REJECTED = "AGENT_ADVANCE_REJECTED";

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
    @Transactional
    public AgentAdvanceResponse requestAdvance(AgentAdvanceSubmitRequest request, Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + agentId));

        if ("INACTIVE".equals(agent.getStatus())) {
            throw new InvalidOperationException("An inactive agent cannot request an advance");
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

        AgentAdvance advance = AgentAdvance.builder()
                .agentId(agentId)
                .amount(amount)
                .advanceDate(businessClock.today())
                .purpose(request.purpose().trim())
                .outstandingBalance(amount)
                .status("PENDING")
                .requestedByAgent(true)
                .build();
        AgentAdvance saved = agentAdvanceRepository.save(advance);

        String message = agent.getName() + " requested an advance of " + kes(amount) + ": " + saved.getPurpose();
        recipients.admins().forEach(admin ->
                notificationService.notifyInApp(admin.getId(), TYPE_REQUESTED, message, saved.getId()));

        return toResponseWithAgentName(saved);
    }

    @Override
    @Transactional
    public AgentAdvanceResponse approveAdvance(Long advanceId, Long adminUserId) {
        AgentAdvance advance = findAdvanceOrThrow(advanceId);
        requirePending(advance, "approved");

        // From here it is a real advance: dated today and recovered from the agent's next payment.
        advance.setStatus("OUTSTANDING");
        advance.setAdvanceDate(businessClock.today());
        advance.setDecidedByUserId(adminUserId);
        advance.setDecidedAt(businessClock.now());
        AgentAdvance saved = agentAdvanceRepository.save(advance);

        notificationService.resolveByReference(TYPE_REQUESTED, saved.getId());
        recipients.agentLogin(saved.getAgentId()).ifPresent(agentUser ->
                notificationService.notifyInApp(agentUser.getId(), TYPE_APPROVED,
                        "Your advance of " + kes(saved.getAmount()) + " was approved. It will be deducted from your next payment.",
                        saved.getId()));

        return toResponseWithAgentName(saved);
    }

    @Override
    @Transactional
    public AgentAdvanceResponse rejectAdvance(Long advanceId, String reason, Long adminUserId) {
        AgentAdvance advance = findAdvanceOrThrow(advanceId);
        requirePending(advance, "rejected");

        advance.setStatus("REJECTED");
        advance.setOutstandingBalance(BigDecimal.ZERO);   // nothing was given, so nothing is owed
        advance.setRejectionReason(reason.trim());
        advance.setDecidedByUserId(adminUserId);
        advance.setDecidedAt(businessClock.now());
        AgentAdvance saved = agentAdvanceRepository.save(advance);

        notificationService.resolveByReference(TYPE_REQUESTED, saved.getId());
        recipients.agentLogin(saved.getAgentId()).ifPresent(agentUser ->
                notificationService.notifyInApp(agentUser.getId(), TYPE_REJECTED,
                        "Your advance of " + kes(saved.getAmount()) + " was rejected: " + saved.getRejectionReason(),
                        saved.getId()));

        return toResponseWithAgentName(saved);
    }

    @Override
    public AgentAdvanceResponse recoverAmount(Long advanceId, AgentAdvanceRecoveryRequest request) {
        AgentAdvance advance = findAdvanceOrThrow(advanceId);
        if ("PENDING".equals(advance.getStatus()) || "REJECTED".equals(advance.getStatus())) {
            throw new InvalidOperationException("This advance has not been approved, so nothing can be recovered from it");
        }
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

    private void requirePending(AgentAdvance advance, String action) {
        if (!"PENDING".equals(advance.getStatus())) {
            throw new InvalidOperationException("Only a pending advance request can be " + action);
        }
    }

    private static String kes(BigDecimal amount) {
        return String.format(Locale.US, "KES %,.2f", amount);
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