// service/offday/impl/OffDayServiceImpl.java
package com.company.hrpayroll.service.offday.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.offday.OffDayRequest;
import com.company.hrpayroll.dto.request.offday.OffDayUpdateRequest;
import com.company.hrpayroll.dto.response.offday.OffDayResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.auth.Role;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Days off: single dates, at most {@link #MAX_PER_MONTH} per employee in a calendar month (e.g. the 3rd and the
 * 16th; they may be consecutive, but are always two separate days, never a range).
 *
 * The agent gives their own employees days off. When the agent isn't available, the Branch Manager (their branch's
 * employees) or the Admin (anyone) does it instead. Nothing here touches attendance: a terminal still accepts a
 * clock-in on an off day, and days worked stay as recorded.
 */
@Service
@RequiredArgsConstructor
public class OffDayServiceImpl implements OffDayService {

    private static final int MAX_RANGE_DAYS = 92;

    private final OffDayRepository offDayRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final AgentRepository agentRepository;
    private final BusinessClock businessClock;

    @Override
    @Transactional
    public List<OffDayResponse> grant(Actor actor, OffDayRequest request) {
        Employee employee = employeeFor(request.employeeId());
        checkMayManage(actor, employee);
        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new InvalidOperationException("This employee is not active");
        }

        Set<LocalDate> dates = new TreeSet<>(request.dates());
        if (dates.size() != request.dates().size()) {
            throw new InvalidOperationException("Choose two different days");
        }
        for (LocalDate date : dates) {
            if (offDayRepository.existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(employee.getId(), date, date)) {
                throw new InvalidOperationException(employee.getFullName() + " is already off on " + date);
            }
        }
        Map<YearMonth, Integer> adding = new TreeMap<>();
        dates.forEach(date -> adding.merge(YearMonth.from(date), 1, Integer::sum));
        adding.forEach((month, count) -> checkMonthLimit(employee, month, count, null));

        String reason = clean(request.reason());
        List<OffDay> saved = new ArrayList<>();
        for (LocalDate date : dates) {
            saved.add(offDayRepository.save(OffDay.builder()
                    .employeeId(employee.getId())
                    .agentId(employee.getAgentId())
                    .startDate(date)
                    .endDate(date)
                    .reason(reason)
                    .build()));
        }
        return toResponses(saved);
    }

    // Moves one off day to another date. One that has already passed stays on record as it was.
    @Override
    @Transactional
    public OffDayResponse update(Actor actor, Long offDayId, OffDayUpdateRequest request) {
        OffDay offDay = offDayFor(offDayId);
        Employee employee = employeeFor(offDay.getEmployeeId());
        checkMayManage(actor, employee);

        LocalDate today = businessClock.today();
        if (offDay.getEndDate().isBefore(today)) {
            throw new InvalidOperationException("An off day that has already passed cannot be changed");
        }
        if (request.date().isBefore(today)) {
            throw new InvalidOperationException("Choose today or a later day");
        }
        boolean takenByAnother = offDayRepository
                .findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(employee.getId(), request.date(), request.date())
                .stream().anyMatch(other -> !other.getId().equals(offDay.getId()));
        if (takenByAnother) {
            throw new InvalidOperationException(employee.getFullName() + " is already off on " + request.date());
        }
        checkMonthLimit(employee, YearMonth.from(request.date()), 1, offDay.getId());

        offDay.setStartDate(request.date());
        offDay.setEndDate(request.date());
        offDay.setReason(clean(request.reason()));
        return toResponses(List.of(offDayRepository.save(offDay))).get(0);
    }

    // A day that has fully passed stays on record, so the history cannot be rewritten afterwards.
    @Override
    @Transactional
    public void remove(Actor actor, Long offDayId) {
        OffDay offDay = offDayFor(offDayId);
        checkMayManage(actor, employeeFor(offDay.getEmployeeId()));
        if (offDay.getEndDate().isBefore(businessClock.today())) {
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

    // An off day belongs to the branch its employee works at.
    @Override
    public List<OffDayResponse> getForBranch(Long branchId, LocalDate from, LocalDate to) {
        return getAll(from, to).stream()
                .filter(offDay -> branchId != null && branchId.equals(offDay.branchId()))
                .toList();
    }

    // ------------------------------------------------------------------------------------------------

    private void checkMayManage(Actor actor, Employee employee) {
        Role role = actor.role();
        boolean allowed = switch (role) {
            case ADMIN, SUPER_ADMIN -> true;
            case BRANCH_MANAGER -> actor.branchId() != null && actor.branchId().equals(employee.getBranchId());
            case AGENT -> actor.agentId() != null && actor.agentId().equals(employee.getAgentId());
            default -> false;
        };
        if (!allowed) {
            throw new InvalidOperationException(switch (role) {
                case BRANCH_MANAGER -> "You can only give days off to your own branch's employees";
                case AGENT -> "You can only give days off to your own employees";
                default -> "You cannot give days off";
            });
        }
    }

    // The days the employee already has off in that month (a day of an older range counts too), plus the new ones.
    private void checkMonthLimit(Employee employee, YearMonth month, int adding, Long ignoreId) {
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        long already = offDayRepository
                .findByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(employee.getId(), last, first)
                .stream()
                .filter(row -> !Objects.equals(row.getId(), ignoreId))
                .mapToLong(row -> ChronoUnit.DAYS.between(
                        row.getStartDate().isBefore(first) ? first : row.getStartDate(),
                        row.getEndDate().isAfter(last) ? last : row.getEndDate()) + 1)
                .sum();
        if (already + adding > MAX_PER_MONTH) {
            String monthName = month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
            throw new InvalidOperationException(already == 0
                    ? "An employee can have at most " + MAX_PER_MONTH + " off days in a month"
                    : employee.getFullName() + " already has " + already + " of " + MAX_PER_MONTH + " off days in " + monthName
                    + (already >= MAX_PER_MONTH ? ". Change or remove one of them instead." : "."));
        }
    }

    private Employee employeeFor(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    private OffDay offDayFor(Long id) {
        return offDayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Off day not found with id: " + id));
    }

    private static String clean(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
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
        Set<Long> agentIds = offDays.stream().map(OffDay::getAgentId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Employee> employees = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, String> agents = agentRepository.findAllById(agentIds).stream()
                .collect(Collectors.toMap(Agent::getId, Agent::getName));
        Set<Long> branchIds = employees.values().stream().map(Employee::getBranchId).filter(Objects::nonNull).collect(Collectors.toSet());
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
                    offDay.getAgentId() != null ? agents.get(offDay.getAgentId()) : null,
                    offDay.getStartDate(),
                    offDay.getEndDate(),
                    offDay.getReason()
            );
        }).toList();
    }
}
