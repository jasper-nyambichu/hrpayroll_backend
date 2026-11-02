// service/dayswork/impl/DaysWorkedServiceImpl.java
package com.company.hrpayroll.service.dayswork.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.dayswork.DaysWorkedAdjustmentRequest;
import com.company.hrpayroll.dto.request.dayswork.DaysWorkedLockRequest;
import com.company.hrpayroll.dto.response.dayswork.DaysWorkedResponse;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.dayswork.DaysWorkedRecord;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.mapper.dayswork.DaysWorkedMapper;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.dayswork.DaysWorkedRecordRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.settlement.AgentSettlementRepository;
import com.company.hrpayroll.service.dayswork.DaysWorkedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DaysWorkedServiceImpl implements DaysWorkedService {

    private static final int MAX_PERIOD_DAYS = 31;

    // A day counts as worked when the employee actually clocked in and was not marked absent or on leave.
    private static final Set<String> WORKED_STATUSES = Set.of("PRESENT", "LATE", "OVERTIME");

    private final DaysWorkedRecordRepository daysWorkedRecordRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AgentSettlementRepository agentSettlementRepository;
    private final DaysWorkedMapper daysWorkedMapper;
    private final BusinessClock businessClock;

    @Override
    public List<DaysWorkedResponse> getForPeriod(Long branchId, LocalDate periodStart, LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);

        List<Employee> employees = activeEmployeesOfBranch(branchId);
        if (employees.isEmpty()) {
            return List.of();
        }

        Map<Long, DaysWorkedRecord> locked = daysWorkedRecordRepository
                .findByBranchIdAndPeriodStartAndPeriodEnd(branchId, periodStart, periodEnd).stream()
                .collect(Collectors.toMap(DaysWorkedRecord::getEmployeeId, Function.identity(), (a, b) -> a));

        Map<Long, Integer> systemDays = countSystemDays(
                employees.stream().map(Employee::getId).toList(), periodStart, periodEnd);

        return employees.stream()
                .map(employee -> {
                    DaysWorkedRecord record = locked.get(employee.getId());
                    return record != null
                            ? daysWorkedMapper.toResponse(record, employee)
                            : daysWorkedMapper.toOpenResponse(
                            employee, periodStart, periodEnd, systemDays.getOrDefault(employee.getId(), 0));
                })
                .toList();
    }

    @Override
    @Transactional
    public List<DaysWorkedResponse> lockPeriod(DaysWorkedLockRequest request, Long lockedByUserId) {
        LocalDate start = request.periodStart();
        LocalDate end = request.periodEnd();
        validatePeriod(start, end);

        List<Employee> employees = activeEmployeesOfBranch(request.branchId());
        if (employees.isEmpty()) {
            throw new InvalidOperationException("This branch has no active employees to lock days for");
        }
        List<Long> employeeIds = employees.stream().map(Employee::getId).toList();

        // A day must never be counted in two periods, nor a period locked twice.
        if (!daysWorkedRecordRepository
                .findByEmployeeIdInAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(employeeIds, end, start)
                .isEmpty()) {
            throw new InvalidOperationException(
                    "Days are already locked for part of this period. Unlock them first to lock again.");
        }

        long periodLength = ChronoUnit.DAYS.between(start, end) + 1;
        Map<Long, DaysWorkedAdjustmentRequest> adjustments = new HashMap<>();
        Set<Long> branchEmployeeIds = new HashSet<>(employeeIds);
        if (request.adjustments() != null) {
            for (DaysWorkedAdjustmentRequest adjustment : request.adjustments()) {
                if (!branchEmployeeIds.contains(adjustment.employeeId())) {
                    throw new InvalidOperationException(
                            "Employee " + adjustment.employeeId() + " is not an active employee of this branch");
                }
                if (adjustment.daysWorked() > periodLength) {
                    throw new InvalidOperationException(
                            "Days worked cannot exceed the " + periodLength + " days in the period");
                }
                adjustments.put(adjustment.employeeId(), adjustment);
            }
        }

        Map<Long, Integer> systemDays = countSystemDays(employeeIds, start, end);
        var lockedAt = businessClock.now();

        List<DaysWorkedRecord> records = employees.stream()
                .map(employee -> {
                    int fromAttendance = systemDays.getOrDefault(employee.getId(), 0);
                    DaysWorkedAdjustmentRequest adjustment = adjustments.get(employee.getId());
                    boolean adjusted = adjustment != null && adjustment.daysWorked() != fromAttendance;

                    return DaysWorkedRecord.builder()
                            .employeeId(employee.getId())
                            .branchId(request.branchId())
                            .periodStart(start)
                            .periodEnd(end)
                            .systemDays(fromAttendance)
                            .daysWorked(adjustment != null ? adjustment.daysWorked() : fromAttendance)
                            .adjusted(adjusted)
                            .adjustmentReason(adjusted ? adjustment.reason().trim() : null)
                            .lockedByUserId(lockedByUserId)
                            .lockedAt(lockedAt)
                            .build();
                })
                .toList();

        Map<Long, Employee> employeesById = employees.stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));

        return daysWorkedRecordRepository.saveAll(records).stream()
                .map(record -> daysWorkedMapper.toResponse(record, employeesById.get(record.getEmployeeId())))
                .toList();
    }

    @Override
    @Transactional
    public void unlockPeriod(Long branchId, LocalDate periodStart, LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);

        List<DaysWorkedRecord> records = daysWorkedRecordRepository
                .findByBranchIdAndPeriodStartAndPeriodEnd(branchId, periodStart, periodEnd);
        if (records.isEmpty()) {
            throw new InvalidOperationException("No locked days found for this branch and period");
        }

        // Once an agent has been paid for the period, the figure is history: reopening it would let
        // the days be changed after the money has moved.
        List<Long> agentIds = employeeRepository.findAllById(records.stream().map(DaysWorkedRecord::getEmployeeId).toList()).stream()
                .map(employee -> employee.getAgentId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (!agentIds.isEmpty() && !agentSettlementRepository.findAgentIdsPaidForOverlappingPeriod(agentIds, periodStart, periodEnd).isEmpty()) {
            throw new InvalidOperationException(
                    "Agents have already been paid for this period, so its days worked can no longer be unlocked");
        }
        daysWorkedRecordRepository.deleteAll(records);
    }

    @Override
    public Map<Long, Integer> getLockedDays(List<Long> employeeIds, LocalDate periodStart, LocalDate periodEnd) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Map.of();
        }
        return daysWorkedRecordRepository
                .findByEmployeeIdInAndPeriodStartAndPeriodEnd(employeeIds, periodStart, periodEnd).stream()
                .collect(Collectors.toMap(DaysWorkedRecord::getEmployeeId, DaysWorkedRecord::getDaysWorked, (a, b) -> a));
    }

    // ---- helpers ----

    private List<Employee> activeEmployeesOfBranch(Long branchId) {
        return employeeRepository.findByBranchId(branchId).stream()
                .filter(employee -> "ACTIVE".equals(employee.getEmploymentStatus()))
                .sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()))
                .toList();
    }

    // Distinct calendar days with a real clock-in, per employee — two sessions on one day count once.
    private Map<Long, Integer> countSystemDays(List<Long> employeeIds, LocalDate start, LocalDate end) {
        Map<Long, Set<LocalDate>> daysByEmployee = new HashMap<>();
        for (Attendance row : attendanceRepository.findByEmployeeIdInAndAttendanceDateBetween(employeeIds, start, end)) {
            if (row.getClockIn() != null && WORKED_STATUSES.contains(row.getStatus())) {
                daysByEmployee.computeIfAbsent(row.getEmployeeId(), id -> new HashSet<>()).add(row.getAttendanceDate());
            }
        }
        return daysByEmployee.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new InvalidOperationException("A period start and end are required");
        }
        if (end.isBefore(start)) {
            throw new InvalidOperationException("Period end cannot be before period start");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_PERIOD_DAYS) {
            throw new InvalidOperationException("A pay period cannot be longer than " + MAX_PERIOD_DAYS + " days");
        }
    }
}