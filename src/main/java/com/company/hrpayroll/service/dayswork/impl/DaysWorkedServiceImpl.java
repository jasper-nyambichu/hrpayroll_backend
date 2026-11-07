// service/dayswork/impl/DaysWorkedServiceImpl.java
package com.company.hrpayroll.service.dayswork.impl;

import com.company.hrpayroll.dto.response.dayswork.DaysWorkedResponse;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.dayswork.DaysWorkedRecord;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.mapper.dayswork.DaysWorkedMapper;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.dayswork.DaysWorkedRecordRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.dayswork.DaysWorkedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final DaysWorkedMapper daysWorkedMapper;

    @Override
    public List<DaysWorkedResponse> getForPeriod(Long branchId, LocalDate periodStart, LocalDate periodEnd) {
        validatePeriod(periodStart, periodEnd);

        List<Employee> employees = activeEmployeesOfBranch(branchId);
        if (employees.isEmpty()) {
            return List.of();
        }

        // A figure locked before locking was removed still stands for its exact period.
        Map<Long, DaysWorkedRecord> locked = daysWorkedRecordRepository
                .findByEmployeeIdInAndPeriodStartAndPeriodEnd(employees.stream().map(Employee::getId).toList(), periodStart, periodEnd).stream()
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
    public Map<Long, Integer> getDaysForPayment(List<Long> employeeIds, LocalDate periodStart, LocalDate periodEnd) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> days = new HashMap<>(countSystemDays(employeeIds, periodStart, periodEnd));
        employeeIds.forEach(id -> days.putIfAbsent(id, 0));
        // A period locked before locking was removed keeps its (possibly adjusted) figure.
        daysWorkedRecordRepository.findByEmployeeIdInAndPeriodStartAndPeriodEnd(employeeIds, periodStart, periodEnd)
                .forEach(record -> days.put(record.getEmployeeId(), record.getDaysWorked()));
        return days;
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