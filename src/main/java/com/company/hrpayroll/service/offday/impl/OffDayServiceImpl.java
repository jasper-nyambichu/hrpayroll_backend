// service/offday/impl/OffDayServiceImpl.java
package com.company.hrpayroll.service.offday.impl;

import com.company.hrpayroll.dto.request.offday.OffDayRequest;
import com.company.hrpayroll.dto.response.offday.OffDayResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.offday.OffDay;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.offday.OffDayRepository;
import com.company.hrpayroll.service.offday.OffDayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The agent gives their own employees days off; the Admin sees them all. Nothing here touches
 * attendance: a terminal still accepts a clock-in on an off day, and days worked stay as recorded.
 */
@Service
@RequiredArgsConstructor
public class OffDayServiceImpl implements OffDayService {

    private static final int MAX_RANGE_DAYS = 92;

    private final OffDayRepository offDayRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final AgentRepository agentRepository;

    @Override
    public OffDayResponse grant(Long agentId, OffDayRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId()));

        if (!agentId.equals(employee.getAgentId())) {
            throw new InvalidOperationException("You can only give days off to your own employees");
        }
        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new InvalidOperationException("This employee is not active");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new InvalidOperationException("End date cannot be before start date");
        }
        if (offDayRepository.existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employee.getId(), request.endDate(), request.startDate())) {
            throw new InvalidOperationException("This employee already has an off day in those dates");
        }

        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        OffDay saved = offDayRepository.save(OffDay.builder()
                .employeeId(employee.getId())
                .agentId(agentId)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(reason)
                .build());

        return toResponses(List.of(saved)).get(0);
    }

    // A day that has fully passed stays on record, so the history cannot be rewritten afterwards.
    @Override
    public void remove(Long agentId, Long offDayId) {
        OffDay offDay = offDayRepository.findById(offDayId)
                .orElseThrow(() -> new ResourceNotFoundException("Off day not found with id: " + offDayId));

        if (!agentId.equals(offDay.getAgentId())) {
            throw new InvalidOperationException("You can only remove your own employees' off days");
        }
        if (offDay.getEndDate().isBefore(LocalDate.now())) {
            throw new InvalidOperationException("An off day that has already passed cannot be removed");
        }

        offDayRepository.delete(offDay);
    }

    @Override
    public List<OffDayResponse> getAll(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return toResponses(offDayRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(to, from));
    }

    @Override
    public List<OffDayResponse> getForAgent(Long agentId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        return toResponses(offDayRepository.findByAgentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(agentId, to, from));
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new InvalidOperationException("The end date cannot be before the start date");
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new InvalidOperationException("A range can cover at most " + MAX_RANGE_DAYS + " days");
        }
    }

    // One batched lookup per list instead of one query per row.
    private List<OffDayResponse> toResponses(List<OffDay> offDays) {
        Set<Long> employeeIds = offDays.stream().map(OffDay::getEmployeeId).collect(Collectors.toSet());
        Set<Long> agentIds = offDays.stream().map(OffDay::getAgentId).collect(Collectors.toSet());

        Map<Long, Employee> employees = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, String> agents = agentRepository.findAllById(agentIds).stream()
                .collect(Collectors.toMap(Agent::getId, Agent::getName));
        Set<Long> branchIds = employees.values().stream().map(Employee::getBranchId).collect(Collectors.toSet());
        Map<Long, String> branches = branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName));

        return offDays.stream().map(offDay -> {
            Employee employee = employees.get(offDay.getEmployeeId());
            return new OffDayResponse(
                    offDay.getId(),
                    offDay.getEmployeeId(),
                    employee != null ? employee.getEmployeeNumber() : null,
                    employee != null ? employee.getFullName() : null,
                    employee != null ? employee.getBranchId() : null,
                    employee != null ? branches.get(employee.getBranchId()) : null,
                    offDay.getAgentId(),
                    agents.get(offDay.getAgentId()),
                    offDay.getStartDate(),
                    offDay.getEndDate(),
                    offDay.getReason()
            );
        }).toList();
    }
}
