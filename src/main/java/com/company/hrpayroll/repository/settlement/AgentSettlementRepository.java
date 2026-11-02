// repository/settlement/AgentSettlementRepository.java
package com.company.hrpayroll.repository.settlement;

import com.company.hrpayroll.entity.settlement.AgentSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface AgentSettlementRepository extends JpaRepository<AgentSettlement, Long> {

    List<AgentSettlement> findAllByOrderByPeriodEndDescIdDesc();

    List<AgentSettlement> findByAgentIdOrderByPeriodEndDescIdDesc(Long agentId);

    // Agents already paid for any day of this period: paying them again would pay those days twice.
    @Query("SELECT DISTINCT s.agentId FROM AgentSettlement s WHERE s.agentId IN :agentIds "
            + "AND s.periodStart <= :periodEnd AND s.periodEnd >= :periodStart")
    List<Long> findAgentIdsPaidForOverlappingPeriod(
            @Param("agentIds") Collection<Long> agentIds,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );
}
