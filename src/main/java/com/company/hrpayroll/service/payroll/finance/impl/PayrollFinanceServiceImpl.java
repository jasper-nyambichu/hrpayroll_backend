// service/payroll/finance/impl/PayrollFinanceServiceImpl.java
package com.company.hrpayroll.service.payroll.finance.impl;

import com.company.hrpayroll.dto.response.payroll.AgentPayrollSummaryResponse;
import com.company.hrpayroll.dto.response.payroll.FinanceRunSummaryResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.payroll.AgentPayrollSummary;
import com.company.hrpayroll.entity.payroll.PayrollRun;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.payroll.AgentPayrollSummaryMapper;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.payroll.AgentPayrollSummaryRepository;
import com.company.hrpayroll.repository.payroll.PayrollRunRepository;
import com.company.hrpayroll.repository.payroll.PayslipRepository;
import com.company.hrpayroll.service.payroll.finance.PayrollFinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollFinanceServiceImpl implements PayrollFinanceService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayslipRepository payslipRepository;
    private final AgentPayrollSummaryRepository agentPayrollSummaryRepository;
    private final AgentRepository agentRepository;
    private final AgentPayrollSummaryMapper agentPayrollSummaryMapper;

    @Override
    public List<FinanceRunSummaryResponse> getRunSummaries() {
        Map<Long, Object[]> direct = indexByRun(payslipRepository.sumDirectEmployeesByRun());
        Map<Long, Object[]> agents = indexByRun(agentPayrollSummaryRepository.sumByRun());

        return payrollRunRepository.findAllByOrderByPeriodEndDescIdDesc().stream()
                .map(run -> build(run, direct.get(run.getId()), agents.get(run.getId())))
                .toList();
    }

    @Override
    public FinanceRunSummaryResponse getRunSummary(Long runId) {
        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found with id: " + runId));

        Map<Long, Object[]> direct = indexByRun(payslipRepository.sumDirectEmployeesByRun());
        Map<Long, Object[]> agents = indexByRun(agentPayrollSummaryRepository.sumByRun());

        return build(run, direct.get(run.getId()), agents.get(run.getId()));
    }

    @Override
    public List<AgentPayrollSummaryResponse> getAllAgentSummaries() {
        List<AgentPayrollSummary> summaries = agentPayrollSummaryRepository.findAll();

        Set<Long> runIds = summaries.stream().map(AgentPayrollSummary::getPayrollRunId).collect(Collectors.toSet());
        Set<Long> agentIds = summaries.stream().map(AgentPayrollSummary::getAgentId).collect(Collectors.toSet());

        Map<Long, PayrollRun> runs = new HashMap<>();
        payrollRunRepository.findAllById(runIds).forEach(run -> runs.put(run.getId(), run));

        Map<Long, String> agentNames = new HashMap<>();
        for (Agent agent : agentRepository.findAllById(agentIds)) {
            agentNames.put(agent.getId(), agent.getName());
        }

        return summaries.stream()
                .filter(summary -> runs.containsKey(summary.getPayrollRunId()))
                .map(summary -> agentPayrollSummaryMapper.toAgentPayrollSummaryResponse(
                        summary, agentNames.get(summary.getAgentId()), runs.get(summary.getPayrollRunId())))
                .sorted((a, b) -> {
                    int byPeriod = b.periodEnd().compareTo(a.periodEnd());
                    return byPeriod != 0 ? byPeriod : b.id().compareTo(a.id());
                })
                .toList();
    }

    private FinanceRunSummaryResponse build(PayrollRun run, Object[] direct, Object[] agents) {
        BigDecimal directNet = decimal(direct, 2);
        BigDecimal directPaye = decimal(direct, 3);
        BigDecimal directNssf = decimal(direct, 4);
        BigDecimal directShif = decimal(direct, 5);
        BigDecimal directLevy = decimal(direct, 6);
        BigDecimal directNita = decimal(direct, 7);
        BigDecimal agentPayable = decimal(agents, 2);

        // Direct staff are paid their net salary, which already contains their SHIF and NSSF (the firm
        // pays no pension, so there is nothing to remit for those). The company remits only the taxes
        // that are switched on (PAYE, Housing Levy) and the employer NITA. Agent staff are covered by
        // the single agent payment, so their payslips are never added again.
        BigDecimal totalCash = directNet.add(directPaye).add(directLevy).add(directNita).add(agentPayable);

        return new FinanceRunSummaryResponse(
                run.getId(),
                run.getPeriodStart(),
                run.getPeriodEnd(),
                run.getStatus().name(),
                count(direct),
                directNet, directPaye, directNssf, directShif, directLevy, directNita,
                decimal(direct, 8),
                count(agents),
                agentPayable,
                decimal(agents, 3),
                decimal(agents, 4), decimal(agents, 5), decimal(agents, 6), decimal(agents, 7),
                totalCash
        );
    }

    private Map<Long, Object[]> indexByRun(List<Object[]> rows) {
        Map<Long, Object[]> byRun = new HashMap<>();
        for (Object[] row : rows) {
            byRun.put(((Number) row[0]).longValue(), row);
        }
        return byRun;
    }

    private long count(Object[] row) {
        return row == null || row[1] == null ? 0 : ((Number) row[1]).longValue();
    }

    private BigDecimal decimal(Object[] row, int index) {
        if (row == null || row[index] == null) {
            return BigDecimal.ZERO;
        }
        return row[index] instanceof BigDecimal value ? value : new BigDecimal(String.valueOf(row[index]));
    }
}