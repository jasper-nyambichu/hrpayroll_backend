// repository/agent/AgentDirectorIdRepository.java
package com.company.hrpayroll.repository.agent;

import com.company.hrpayroll.entity.agent.AgentDirectorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentDirectorIdRepository extends JpaRepository<AgentDirectorId, Long> {

    Optional<AgentDirectorId> findByAgentId(Long agentId);

    // Counts rather than loads, so listing agents never reads the images.
    @Query("select count(d) > 0 from AgentDirectorId d where d.agentId = :agentId")
    boolean hasPhoto(@Param("agentId") Long agentId);
}
