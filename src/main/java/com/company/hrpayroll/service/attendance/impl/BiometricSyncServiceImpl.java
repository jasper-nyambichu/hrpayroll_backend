// service/attendance/impl/BiometricSyncServiceImpl.java
package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.attendance.BiometricSyncRequest;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.attendance.AttendanceSource;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.attendance.BiometricDeviceMapping;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.leave.LeaveStatus;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceMappingRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.leave.LeaveRequestRepository;
import com.company.hrpayroll.service.attendance.BiometricSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BiometricSyncServiceImpl implements BiometricSyncService {

    // A clock-out may close a session opened up to this long before it (night shifts, scans just after midnight).
    private static final Duration MAX_OPEN_SESSION = Duration.ofHours(20);

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final BiometricDeviceMappingRepository mappingRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final BiometricSyncFailureLogger failureLogger;   // separate bean => REQUIRES_NEW really applies
    private final BusinessClock businessClock;

    @Override
    @Transactional
    public AttendanceResponse syncEvent(BiometricSyncRequest request) {

        // Idempotency: a retried CLOCK_IN *or* CLOCK_OUT returns the record it already produced.
        var existing = attendanceRepository.findByEventId(request.eventId())
                .or(() -> attendanceRepository.findByClockOutEventId(request.eventId()));
        if (existing.isPresent()) {
            String name = employeeRepository.findById(existing.get().getEmployeeId())
                    .map(Employee::getFullName).orElse(null);
            return toResponse(existing.get(), name);
        }

        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(request.deviceSerial()).orElse(null);
        if (device == null) {
            String reason = "Unknown device: " + request.deviceSerial();
            failureLogger.log(request, null, reason);
            throw new ResourceNotFoundException(reason);
        }

        boolean clockIn = "CLOCK_IN".equalsIgnoreCase(request.eventType());
        boolean clockOut = "CLOCK_OUT".equalsIgnoreCase(request.eventType());
        if (!clockIn && !clockOut) {
            throw rejected(request, null, "Unsupported event type: " + request.eventType());
        }

        BiometricDeviceMapping mapping = mappingRepository
                .findByDeviceIdAndTerminalUserId(device.getId(), request.terminalUserId())
                .orElse(null);
        if (mapping == null) {
            throw rejected(request, null, "No employee mapped to terminal user id " + request.terminalUserId()
                    + " on device " + request.deviceSerial());
        }

        Employee employee = employeeRepository.findById(mapping.getEmployeeId()).orElse(null);
        if (employee == null) {
            failureLogger.log(request, mapping.getEmployeeId(), "Employee not found with id: " + mapping.getEmployeeId());
            throw new ResourceNotFoundException("Employee not found with id: " + mapping.getEmployeeId());
        }

        if (!employee.getBranchId().equals(device.getBranchId())) {
            throw rejected(request, employee.getId(), "Employee does not belong to the branch this device is registered to");
        }

        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw rejected(request, employee.getId(), "Employee is not active — attendance cannot be recorded");
        }

        LocalDateTime eventTime = businessClock.resolve(request.timestamp(), request.timestampUtc());
        LocalDate eventDate = eventTime.toLocalDate();

        boolean onApprovedLeaveToday = !leaveRequestRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employee.getId(), LeaveStatus.APPROVED, eventDate, eventDate)
                .isEmpty();
        if (onApprovedLeaveToday) {
            throw rejected(request, employee.getId(), "Employee is on approved leave today — attendance not recorded");
        }

        Attendance attendance;
        try {
            attendance = clockOut
                    ? handleClockOut(employee, eventTime, request)
                    : handleClockIn(employee, eventTime, request, device);
        } catch (InvalidOperationException ex) {
            failureLogger.log(request, employee.getId(), truncate(ex.getMessage()));
            throw ex;
        }

        device.setLastSyncAt(businessClock.now());
        biometricDeviceRepository.save(device);

        return toResponse(attendance, employee.getFullName());
    }

    private Attendance handleClockIn(Employee employee, LocalDateTime eventTime,
                                     BiometricSyncRequest request, BiometricDevice device) {

        boolean alreadyOpen = attendanceRepository
                .findFirstByEmployeeIdAndAttendanceDateAndClockOutIsNullOrderByClockInDesc(
                        employee.getId(), eventTime.toLocalDate())
                .isPresent();
        if (alreadyOpen) {
            throw new InvalidOperationException("Employee already has an open clock-in for today");
        }

        Attendance attendance = Attendance.builder()
                .employeeId(employee.getId())
                .branchId(employee.getBranchId())
                .attendanceDate(eventTime.toLocalDate())
                .clockIn(eventTime)
                .source(AttendanceSource.BIOMETRIC)
                .status("PRESENT")
                .eventId(request.eventId())
                .deviceId(device.getId())
                .verifyMethod(request.verifyMethod())
                .build();

        return attendanceRepository.save(attendance);
    }

    private Attendance handleClockOut(Employee employee, LocalDateTime eventTime, BiometricSyncRequest request) {
        Attendance open = attendanceRepository
                .findFirstByEmployeeIdAndAttendanceDateAndClockOutIsNullOrderByClockInDesc(
                        employee.getId(), eventTime.toLocalDate())
                .or(() -> attendanceRepository
                        .findFirstByEmployeeIdAndClockOutIsNullOrderByClockInDesc(employee.getId())
                        .filter(a -> isRecentOpenSession(a, eventTime)))
                .orElseThrow(() -> new InvalidOperationException(
                        "No open clock-in found for this employee to close"));

        // Never record a clock-out earlier than its clock-in (device clock skew).
        LocalDateTime out = open.getClockIn() != null && eventTime.isBefore(open.getClockIn())
                ? open.getClockIn() : eventTime;

        open.setClockOut(out);
        open.setClockOutEventId(request.eventId());
        return attendanceRepository.save(open);
    }

    private boolean isRecentOpenSession(Attendance a, LocalDateTime eventTime) {
        return a.getClockIn() != null
                && !a.getClockIn().isAfter(eventTime)
                && Duration.between(a.getClockIn(), eventTime).compareTo(MAX_OPEN_SESSION) <= 0;
    }

    private InvalidOperationException rejected(BiometricSyncRequest request, Long employeeId, String reason) {
        failureLogger.log(request, employeeId, truncate(reason));
        return new InvalidOperationException(reason);
    }

    private static String truncate(String s) {
        if (s == null) return "Unknown error";
        return s.length() > 250 ? s.substring(0, 250) : s;
    }

    private AttendanceResponse toResponse(Attendance a, String employeeName) {
        return new AttendanceResponse(
                a.getId(), a.getEmployeeId(), employeeName, a.getBranchId(),
                a.getAttendanceDate(), a.getClockIn(), a.getClockOut(),
                a.getSource().name(), a.getStatus()
        );
    }
}