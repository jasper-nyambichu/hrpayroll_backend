// repository/agent/AgentRepository.java
package com.company.hrpayroll.repository.agent;

import com.company.hrpayroll.entity.agent.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    List<Agent> findByStatus(String status);

    List<Agent> findByIdIn(List<Long> ids);

    List<Agent> findByVatRegisteredTrue();

    List<Agent> findByNameContainingIgnoreCase(String name);
}