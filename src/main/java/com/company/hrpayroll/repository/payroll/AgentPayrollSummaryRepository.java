// repository/payroll/AgentPayrollSummaryRepository.java
package com.company.hrpayroll.repository.payroll;

import com.company.hrpayroll.entity.payroll.AgentPayrollSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentPayrollSummaryRepository extends JpaRepository<AgentPayrollSummary, Long> {

    List<AgentPayrollSummary> findByPayrollRunId(Long payrollRunId);

    List<AgentPayrollSummary> findByAgentId(Long agentId);

    Optional<AgentPayrollSummary> findByPayrollRunIdAndAgentId(Long payrollRunId, Long agentId);

    // Per run, what the agents are owed. Columns: runId, agents, final payable, VAT,
    // PAYE, NSSF, SHIF, housing levy (the statutory amounts the agents remit).
    @Query("select s.payrollRunId, count(s), sum(s.finalAmountPayable), sum(s.vatAmount), " +
            "sum(s.payeTotal), sum(s.nssfTotal), sum(s.shifTotal), sum(s.housingLevyTotal) " +
            "from AgentPayrollSummary s group by s.payrollRunId")
    List<Object[]> sumByRun();
}