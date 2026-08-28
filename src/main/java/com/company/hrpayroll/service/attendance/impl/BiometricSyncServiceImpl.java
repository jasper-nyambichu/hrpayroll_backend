// service/attendance/impl/BiometricSyncServiceImpl.java
package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.dto.request.attendance.BiometricSyncRequest;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.attendance.AttendanceSource;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.attendance.BiometricDeviceMapping;
import com.company.hrpayroll.entity.attendance.BiometricSyncFailure;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.leave.LeaveStatus;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceMappingRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.attendance.BiometricSyncFailureRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.leave.LeaveRequestRepository;
import com.company.hrpayroll.service.attendance.BiometricSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BiometricSyncServiceImpl implements BiometricSyncService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final BiometricDeviceMappingRepository mappingRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final BiometricSyncFailureRepository syncFailureRepository;

    @Override
    @Transactional
    public AttendanceResponse syncEvent(BiometricSyncRequest request) {

        // Idempotency: a retried event after a network blip returns the
        // existing record rather than erroring — the bridge-agent can
        // safely retry without special handling.
        var existing = attendanceRepository.findByEventId(request.eventId());
        if (existing.isPresent()) {
            String existingEmployeeName = employeeRepository.findById(existing.get().getEmployeeId())
                    .map(Employee::getFullName).orElse(null);
            return toResponse(existing.get(), existingEmployeeName);
        }

        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(request.deviceSerial())
                .orElse(null);
        if (device == null) {
            logFailure(request, null, "Unknown device: " + request.deviceSerial());
            throw new ResourceNotFoundException("Unknown device: " + request.deviceSerial());
        }

        // Server-side resolution: the terminal only knows its own internal
        // user id. We resolve it against the mapping table rather than
        // trusting a pre-resolved employeeId from the agent, so the
        // database mapping stays the single source of truth even if an
        // agent's local cache is stale.
        BiometricDeviceMapping mapping = mappingRepository
                .findByDeviceIdAndTerminalUserId(device.getId(), request.terminalUserId())
                .orElse(null);
        if (mapping == null) {
            String reason = "No employee mapped to terminal user id " + request.terminalUserId() +
                    " on device " + request.deviceSerial();
            logFailure(request, null, reason);
            throw new InvalidOperationException(reason);
        }

        Employee employee = employeeRepository.findById(mapping.getEmployeeId()).orElse(null);
        if (employee == null) {
            logFailure(request, mapping.getEmployeeId(), "Employee not found with id: " + mapping.getEmployeeId());
            throw new ResourceNotFoundException("Employee not found with id: " + mapping.getEmployeeId());
        }

        if (!employee.getBranchId().equals(device.getBranchId())) {
            String reason = "Employee does not belong to the branch this device is registered to";
            logFailure(request, employee.getId(), reason);
            throw new InvalidOperationException(reason);
        }

        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            String reason = "Employee is not active — attendance cannot be recorded";
            logFailure(request, employee.getId(), reason);
            throw new InvalidOperationException(reason);
        }

        LocalDate eventDate = request.timestamp().toLocalDate();

        boolean onApprovedLeaveToday = !leaveRequestRepository
                .findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employee.getId(), LeaveStatus.APPROVED, eventDate, eventDate)
                .isEmpty();
        if (onApprovedLeaveToday) {
            String reason = "Employee is on approved leave today — attendance not recorded";
            logFailure(request, employee.getId(), reason);
            throw new InvalidOperationException(reason);
        }

        Attendance attendance;
        try {
            attendance = "CLOCK_OUT".equalsIgnoreCase(request.eventType())
                    ? handleClockOut(employee, eventDate, request)
                    : handleClockIn(employee, eventDate, request, device);
        } catch (InvalidOperationException ex) {
            logFailure(request, employee.getId(), ex.getMessage());
            throw ex;
        }

        device.setLastSyncAt(LocalDateTime.now());
        biometricDeviceRepository.save(device);

        return toResponse(attendance, employee.getFullName());
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
                .verifyMethod(request.verifyMethod())
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

    // REQUIRES_NEW so a failure log survives even though the outer
    // transaction is about to roll back from the exception being thrown.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void logFailure(BiometricSyncRequest request, Long employeeId, String reason) {
        BiometricSyncFailure failure = BiometricSyncFailure.builder()
                .eventId(request.eventId())
                .deviceSerial(request.deviceSerial())
                .employeeId(employeeId)
                .reason(reason)
                .build();
        syncFailureRepository.save(failure);
    }

    private AttendanceResponse toResponse(Attendance a, String employeeName) {
        return new AttendanceResponse(
                a.getId(), a.getEmployeeId(), employeeName, a.getBranchId(),
                a.getAttendanceDate(), a.getClockIn(), a.getClockOut(),
                a.getSource().name(), a.getStatus()
        );
    }
}