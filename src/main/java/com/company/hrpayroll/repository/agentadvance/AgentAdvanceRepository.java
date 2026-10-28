// repository/agentadvance/AgentAdvanceRepository.java
package com.company.hrpayroll.repository.agentadvance;

import com.company.hrpayroll.entity.agentadvance.AgentAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentAdvanceRepository extends JpaRepository<AgentAdvance, Long> {

    List<AgentAdvance> findByAgentId(Long agentId);

    // Outstanding or partially recovered advances — what gets subtracted
    // from the agent's next payroll payable amount.
    List<AgentAdvance> findByAgentIdAndStatusNot(Long agentId, String status);

    List<AgentAdvance> findByAgentIdAndStatusIn(Long agentId, List<String> statuses);

    // Only advances that were actually given: a PENDING or REJECTED request (one an agent asked for but
    // the Admin has not approved) must never be taken off a payroll run.
    default List<AgentAdvance> findRecoverableByAgentId(Long agentId) {
        return findByAgentIdAndStatusIn(agentId, List.of("OUTSTANDING", "PARTIALLY_RECOVERED"));
    }
}