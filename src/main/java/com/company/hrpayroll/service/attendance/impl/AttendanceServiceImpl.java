// service/attendance/impl/AttendanceServiceImpl.java
package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.audit.AuditDetails;
import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.attendance.AttendanceRequest;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;
import com.company.hrpayroll.dto.response.attendance.ManualCheckInResult;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.attendance.AttendanceSource;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.leave.LeaveStatus;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.attendance.AttendanceMapper;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.leave.LeaveRequestRepository;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.attendance.AttendanceService;
import com.company.hrpayroll.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    // Marks a manager's check-in apart from a fingerprint on the attendance row.
    private static final String MANUAL_VERIFY_METHOD = "MANAGER_MANUAL";

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceMapper attendanceMapper;
    private final LeaveRequestRepository leaveRequestRepository;
    private final BusinessClock businessClock;
    private final AccessGuard accessGuard;
    private final AuditService auditService;

    @Override
    @Transactional
    public List<ManualCheckInResult> checkInAtMyBranch(List<Long> employeeIds) {
        Long branchId = accessGuard.currentBranchId();
        if (branchId == null) {
            throw new InvalidOperationException("This login isn't assigned to a branch. Ask your administrator.");
        }

        // The server's clock, never the PC's, so a manual check-in can't be backdated.
        LocalDateTime now = businessClock.now();
        LocalDate today = now.toLocalDate();
        var zone = businessClock.zone();
        Long managerUserId = accessGuard.currentUserId();

        List<ManualCheckInResult> results = new ArrayList<>();
        for (Long employeeId : new LinkedHashSet<>(employeeIds)) {
            if (employeeId == null) continue;

            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null || !branchId.equals(employee.getBranchId())) {
                results.add(refused(employeeId, null, "Not an employee of your branch"));
                continue;
            }
            if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
                results.add(refused(employeeId, employee.getFullName(), "Not active, so attendance can't be recorded"));
                continue;
            }
            boolean onLeave = !leaveRequestRepository
                    .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            employeeId, LeaveStatus.APPROVED, today, today)
                    .isEmpty();
            if (onLeave) {
                results.add(refused(employeeId, employee.getFullName(), "On approved leave today"));
                continue;
            }
            boolean alreadyIn = attendanceRepository
                    .findFirstByEmployeeIdAndAttendanceDateAndClockOutIsNullOrderByClockInDesc(employeeId, today)
                    .isPresent();
            if (alreadyIn) {
                results.add(refused(employeeId, employee.getFullName(), "Already checked in today"));
                continue;
            }

            Attendance saved = attendanceRepository.save(Attendance.builder()
                    .employeeId(employeeId)
                    .branchId(branchId)
                    .attendanceDate(today)
                    .clockIn(now)
                    .source(AttendanceSource.MANUAL)
                    .status("PRESENT")
                    .verifyMethod(MANUAL_VERIFY_METHOD)
                    .build());

            // Who checked them in is kept in the audit trail, one row per employee.
            auditService.record(managerUserId, "MANUAL_CHECK_IN", "Attendance", saved.getId(),
                    AuditDetails.withRequest("employeeId", String.valueOf(employeeId), "branchId", String.valueOf(branchId)));

            results.add(new ManualCheckInResult(employeeId, employee.getFullName(), true, saved.getId(),
                    now, now.atZone(zone).toInstant(), "Checked in"));
        }
        return results;
    }

    private static ManualCheckInResult refused(Long employeeId, String name, String message) {
        return new ManualCheckInResult(employeeId, name, false, null, null, null, message);
    }

    @Override
    public AttendanceResponse clockInManually(AttendanceRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + request.employeeId()));

        LocalDate date = request.clockIn().toLocalDate();

        boolean alreadyOpen = attendanceRepository
                .findByEmployeeIdAndAttendanceDateAndClockOutIsNull(request.employeeId(), date)
                .isPresent();
        if (alreadyOpen) {
            throw new InvalidOperationException("This employee already has an open clock-in for today");
        }

        Attendance attendance = Attendance.builder()
                .employeeId(employee.getId())
                .branchId(employee.getBranchId())
                .attendanceDate(date)
                .clockIn(request.clockIn())
                .clockOut(request.clockOut())
                .source(AttendanceSource.MANUAL)
                .status("PRESENT")
                .build();

        Attendance saved = attendanceRepository.save(attendance);
        return attendanceMapper.toAttendanceResponse(saved, employee.getFullName());
    }

    @Override
    public AttendanceResponse clockOutManually(Long attendanceId, LocalDateTime clockOutTime) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with id: " + attendanceId));

        if (attendance.getClockOut() != null) {
            throw new InvalidOperationException("This attendance record is already clocked out");
        }

        attendance.setClockOut(clockOutTime);
        Attendance saved = attendanceRepository.save(attendance);

        String employeeName = employeeRepository.findById(saved.getEmployeeId())
                .map(Employee::getFullName).orElse(null);
        return attendanceMapper.toAttendanceResponse(saved, employeeName);
    }

    @Override
    public List<AttendanceResponse> getAttendanceForEmployee(Long employeeId, LocalDate start, LocalDate end) {
        String employeeName = employeeRepository.findById(employeeId)
                .map(Employee::getFullName).orElse(null);

        return attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(employeeId, start, end).stream()
                .map(a -> attendanceMapper.toAttendanceResponse(a, employeeName))
                .toList();
    }

    @Override
    public List<AttendanceResponse> getAttendanceForBranch(Long branchId, LocalDate start, LocalDate end) {
        return attendanceRepository.findByBranchIdAndAttendanceDateBetween(branchId, start, end).stream()
                .map(a -> {
                    String employeeName = employeeRepository.findById(a.getEmployeeId())
                            .map(Employee::getFullName).orElse(null);
                    return attendanceMapper.toAttendanceResponse(a, employeeName);
                })
                .toList();
    }

    @Override
    public List<AttendanceResponse> getLiveAttendanceForBranch(Long branchId, LocalDateTime since) {
        return List.of();
    }
}