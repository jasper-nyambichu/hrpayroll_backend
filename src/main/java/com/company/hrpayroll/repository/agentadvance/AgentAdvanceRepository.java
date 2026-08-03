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

    default List<AgentAdvance> findRecoverableByAgentId(Long agentId) {
        return findByAgentIdAndStatusNot(agentId, "FULLY_RECOVERED");
    }
}