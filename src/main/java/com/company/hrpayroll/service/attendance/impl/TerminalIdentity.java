package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.attendance.BiometricDeviceMapping;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.repository.attendance.BiometricDeviceMappingRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Who a terminal user id means. An enrolment belongs to the employee, not the terminal it was made on, so
 * the id they were given keeps working at any branch's terminal and nobody enrols again after a move.
 * Whether they may clock in at that terminal is still decided by the branch they now work at.
 */
@Component
@RequiredArgsConstructor
public class TerminalIdentity {

    private static final Comparator<BiometricDeviceMapping> LATEST_FIRST =
            Comparator.comparing(BiometricDeviceMapping::getEnrolledAt).thenComparing(BiometricDeviceMapping::getId).reversed();

    private final BiometricDeviceMappingRepository mappingRepository;
    private final EmployeeRepository employeeRepository;

    // This terminal's own mapping wins. Otherwise the id must mean exactly one person across the company,
    // because a wrong guess would record attendance against the wrong employee.
    public Optional<Long> employeeFor(Long deviceId, String terminalUserId) {
        Optional<BiometricDeviceMapping> own = mappingRepository.findByDeviceIdAndTerminalUserId(deviceId, terminalUserId);
        if (own.isPresent()) {
            return own.map(BiometricDeviceMapping::getEmployeeId);
        }

        List<Long> employees = mappingRepository.findByTerminalUserId(terminalUserId).stream()
                .map(BiometricDeviceMapping::getEmployeeId)
                .distinct()
                .toList();
        return employees.size() == 1 ? Optional.of(employees.get(0)) : Optional.empty();
    }

    // Terminal user id of everyone active who now works at this device's branch and is enrolled anywhere.
    // Anyone whose id is already taken on this terminal by somebody else is left out, not guessed at.
    // Someone with several ids gets their latest enrolment, so a PC is always told the same one.
    public Map<Long, String> idsAtBranchOf(BiometricDevice device) {
        List<Long> employeeIds = employeeRepository.findByBranchId(device.getBranchId()).stream()
                .filter(employee -> "ACTIVE".equals(employee.getEmploymentStatus()))
                .map(Employee::getId)
                .toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }

        List<BiometricDeviceMapping> ownRows = mappingRepository.findByDeviceId(device.getId());
        Map<String, Long> takenHere = new HashMap<>();
        ownRows.forEach(row -> takenHere.put(row.getTerminalUserId(), row.getEmployeeId()));

        Set<Long> atBranch = new HashSet<>(employeeIds);
        Map<Long, String> ids = new LinkedHashMap<>();
        ownRows.stream()
                .filter(row -> atBranch.contains(row.getEmployeeId()))
                .sorted(LATEST_FIRST)
                .forEach(row -> ids.putIfAbsent(row.getEmployeeId(), row.getTerminalUserId()));

        List<BiometricDeviceMapping> everywhere = mappingRepository.findByEmployeeIdIn(employeeIds).stream()
                .sorted(LATEST_FIRST)
                .toList();
        for (BiometricDeviceMapping row : everywhere) {
            Long holder = takenHere.get(row.getTerminalUserId());
            if (holder == null || holder.equals(row.getEmployeeId())) {
                ids.putIfAbsent(row.getEmployeeId(), row.getTerminalUserId());
            }
        }
        return ids;
    }
}
