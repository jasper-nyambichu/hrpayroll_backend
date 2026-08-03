// service/attendance/impl/BiometricSyncServiceImpl.java
package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.dto.request.attendance.BiometricSyncRequest;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.attendance.AttendanceSource;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.leave.LeaveStatus;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.attendance.AttendanceMapper;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.leave.LeaveRequestRepository;
import com.company.hrpayroll.service.attendance.BiometricSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BiometricSyncServiceImpl implements BiometricSyncService {

    // Below this confidence, the match is treated as "not confident enough"
    // rather than recorded — a low-confidence guess is worse than no record.
    private static final double MIN_MATCH_CONFIDENCE = 0.85;

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceMapper attendanceMapper;

    @Override
    @Transactional
    public AttendanceResponse syncEvent(BiometricSyncRequest request) {

        // Idempotency: if this exact event was already synced (a retry after
        // a network blip), return the existing record instead of erroring —
        // the bridge-agent should be able to safely retry without special
        // handling on its end.
        var existing = attendanceRepository.findByEventId(request.eventId());
        if (existing.isPresent()) {
            String existingEmployeeName = employeeRepository.findById(existing.get().getEmployeeId())
                    .map(Employee::getFullName).orElse(null);
            return attendanceMapper.toAttendanceResponse(existing.get(), existingEmployeeName);
        }

        if (request.matchConfidenceScore() < MIN_MATCH_CONFIDENCE) {
            throw new InvalidOperationException(
                    "Match confidence too low (" + request.matchConfidenceScore() + "); scan rejected");
        }

        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(request.deviceSerial())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown device: " + request.deviceSerial()));

        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId()));

        if (!employee.getBranchId().equals(device.getBranchId())) {
            throw new InvalidOperationException(
                    "Employee does not belong to the branch this device is registered to");
        }

        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new InvalidOperationException("Employee is not active — attendance cannot be recorded");
        }

        LocalDate eventDate = request.timestamp().toLocalDate();

        boolean onApprovedLeaveToday = !leaveRequestRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employee.getId(), LeaveStatus.APPROVED, eventDate, eventDate)
                .isEmpty();
        if (onApprovedLeaveToday) {
            throw new InvalidOperationException("Employee is on approved leave today — attendance not recorded");
        }

        Attendance attendance = "CLOCK_OUT".equalsIgnoreCase(request.eventType())
                ? handleClockOut(employee, eventDate, request)
                : handleClockIn(employee, eventDate, request, device);

        device.setLastSyncAt(LocalDateTime.now());
        biometricDeviceRepository.save(device);

        return attendanceMapper.toAttendanceResponse(attendance, employee.getFullName());
    }

    private Attendance handleClockIn(
            Employee employee, LocalDate eventDate, BiometricSyncRequest request, BiometricDevice device) {

        boolean alreadyOpen = attendanceRepository
                .findByEmployeeIdAndAttendanceDateAndClockOutIsNull(employee.getId(), eventDate)
                .isPresent();
        if (alreadyOpen) {
            throw new InvalidOperationException("Employee already has an open clock-in for today");
        }

        Attendance attendance = Attendance.builder()
                .employeeId(employee.getId())
                .branchId(employee.getBranchId())
                .attendanceDate(eventDate)
                .clockIn(request.timestamp())
                .source(AttendanceSource.BIOMETRIC)
                .status("PRESENT")
                .eventId(request.eventId())
                .deviceId(device.getId())
                .matchConfidenceScore(request.matchConfidenceScore())
                .build();

        return attendanceRepository.save(attendance);
    }

    private Attendance handleClockOut(Employee employee, LocalDate eventDate, BiometricSyncRequest request) {
        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDateAndClockOutIsNull(employee.getId(), eventDate)
                .orElseThrow(() -> new InvalidOperationException(
                        "No open clock-in found for this employee today to close"));

        attendance.setClockOut(request.timestamp());
        return attendanceRepository.save(attendance);
    }
}