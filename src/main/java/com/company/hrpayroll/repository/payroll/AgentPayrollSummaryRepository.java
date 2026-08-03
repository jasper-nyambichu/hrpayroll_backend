// repository/payroll/AgentPayrollSummaryRepository.java
package com.company.hrpayroll.repository.payroll;

import com.company.hrpayroll.entity.payroll.AgentPayrollSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentPayrollSummaryRepository extends JpaRepository<AgentPayrollSummary, Long> {

    List<AgentPayrollSummary> findByPayrollRunId(Long payrollRunId);

    List<AgentPayrollSummary> findByAgentId(Long agentId);

    Optional<AgentPayrollSummary> findByPayrollRunIdAndAgentId(Long payrollRunId, Long agentId);
}