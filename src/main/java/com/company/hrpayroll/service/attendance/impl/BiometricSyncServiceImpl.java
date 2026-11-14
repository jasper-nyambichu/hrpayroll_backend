// service/attendance/impl/BiometricSyncServiceImpl.java
package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.attendance.BiometricSyncRequest;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.attendance.AttendanceCheckout;
import com.company.hrpayroll.entity.attendance.AttendanceSource;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.leave.LeaveStatus;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.AttendanceCheckoutRepository;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.leave.LeaveRequestRepository;
import com.company.hrpayroll.service.attendance.BiometricSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BiometricSyncServiceImpl implements BiometricSyncService {

    // A clock-out may close a session opened up to this long before it (night shifts, scans just after midnight).
    private static final Duration MAX_OPEN_SESSION = Duration.ofHours(20);

    // Coming back from lunch after this long is recorded as late.
    private static final Duration LUNCH_ALLOWANCE = Duration.ofMinutes(30);

    private static final int MAX_REASON_LENGTH = 200;

    private final AttendanceRepository attendanceRepository;
    private final AttendanceCheckoutRepository checkoutRepository;
    private final EmployeeRepository employeeRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final TerminalIdentity terminalIdentity;
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

        // The same for a lunch or temporary check-out, and for coming back from one.
        var existingBreak = checkoutRepository.findByEventId(request.eventId())
                .or(() -> checkoutRepository.findByReturnEventId(request.eventId()))
                .flatMap(c -> attendanceRepository.findById(c.getAttendanceId()));
        if (existingBreak.isPresent()) {
            String name = employeeRepository.findById(existingBreak.get().getEmployeeId())
                    .map(Employee::getFullName).orElse(null);
            return toResponse(existingBreak.get(), name);
        }

        BiometricDevice device = biometricDeviceRepository.findByDeviceSerial(request.deviceSerial()).orElse(null);
        if (device == null) {
            String reason = "Unknown device: " + request.deviceSerial();
            failureLogger.log(request, null, reason);
            throw new ResourceNotFoundException(reason);
        }

        boolean clockIn = "CLOCK_IN".equalsIgnoreCase(request.eventType());
        boolean clockOut = "CLOCK_OUT".equalsIgnoreCase(request.eventType());
        boolean lunchOut = "LUNCH_OUT".equalsIgnoreCase(request.eventType());
        boolean emergencyOut = "EMERGENCY_OUT".equalsIgnoreCase(request.eventType());
        boolean breakReturn = "BREAK_RETURN".equalsIgnoreCase(request.eventType());
        if (!clockIn && !clockOut && !lunchOut && !emergencyOut && !breakReturn) {
            throw rejected(request, null, "Unsupported event type: " + request.eventType());
        }

        Long employeeId = terminalIdentity.employeeFor(device.getId(), request.terminalUserId()).orElse(null);
        if (employeeId == null) {
            throw rejected(request, null, "No employee mapped to terminal user id " + request.terminalUserId()
                    + " on device " + request.deviceSerial());
        }

        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            failureLogger.log(request, employeeId, "Employee not found with id: " + employeeId);
            throw new ResourceNotFoundException("Employee not found with id: " + employeeId);
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
            if (clockOut) {
                attendance = handleClockOut(employee, eventTime, request);
            } else if (clockIn) {
                attendance = handleClockIn(employee, eventTime, request, device);
            } else if (breakReturn) {
                attendance = handleBreakReturn(employee, eventTime, request);
            } else {
                attendance = handleBreakOut(employee, eventTime, request, lunchOut);
            }
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

        // Already checked in (typically on the branch's other PC): nothing to add, and not an error. Two PCs
        // can each record the same step before they hear of each other; the first one to arrive counts.
        var alreadyOpen = attendanceRepository
                .findFirstByEmployeeIdAndAttendanceDateAndClockOutIsNullOrderByClockInDesc(
                        employee.getId(), eventTime.toLocalDate());
        if (alreadyOpen.isPresent()) {
            log.info("Check-in {} for employee {} absorbed: already checked in", request.eventId(), employee.getId());
            return alreadyOpen.get();
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

    private Attendance handleBreakOut(Employee employee, LocalDateTime eventTime, BiometricSyncRequest request, boolean lunch) {
        Attendance open = openSession(employee, eventTime, "No open clock-in found for this employee to step out from");

        // Already out (the other PC recorded it first): the step has happened, so this copy adds nothing.
        if (checkoutRepository.findFirstByAttendanceIdAndCheckedInAtIsNullOrderByCheckedOutAtDesc(open.getId()).isPresent()) {
            log.info("Step-out {} for employee {} absorbed: already out", request.eventId(), employee.getId());
            return open;
        }

        LocalDateTime expectedBack;
        String reason = null;
        if (lunch) {
            expectedBack = eventTime.plus(LUNCH_ALLOWANCE);
        } else {
            reason = request.reason() == null ? "" : request.reason().trim();
            if (reason.isEmpty()) {
                throw new InvalidOperationException("A reason is required for a temporary check-out");
            }
            if (reason.length() > MAX_REASON_LENGTH) {
                reason = reason.substring(0, MAX_REASON_LENGTH);
            }
            if (request.expectedBackAtUtc() == null) {
                throw new InvalidOperationException("A return time is required for a temporary check-out");
            }
            expectedBack = businessClock.resolve(null, request.expectedBackAtUtc());
            if (!expectedBack.isAfter(eventTime) || !expectedBack.toLocalDate().equals(eventTime.toLocalDate())) {
                throw new InvalidOperationException("The return time must be later the same day");
            }
        }

        checkoutRepository.save(AttendanceCheckout.builder()
                .attendanceId(open.getId())
                .checkType(lunch ? AttendanceCheckout.LUNCH : AttendanceCheckout.EMERGENCY)
                .checkedOutAt(eventTime)
                .expectedBackAt(expectedBack)
                .reason(reason)
                .eventId(request.eventId())
                .build());

        return open;
    }

    private Attendance handleBreakReturn(Employee employee, LocalDateTime eventTime, BiometricSyncRequest request) {
        Attendance open = openSession(employee, eventTime, "No open clock-in found for this employee to return to");

        AttendanceCheckout out = checkoutRepository
                .findFirstByAttendanceIdAndCheckedInAtIsNullOrderByCheckedOutAtDesc(open.getId())
                .orElse(null);
        if (out == null) {
            // Back already (the other PC recorded the return first): nothing more to record.
            boolean returnedEarlier = checkoutRepository.findByAttendanceIdInOrderByCheckedOutAtAsc(List.of(open.getId())).stream()
                    .anyMatch(c -> c.getCheckedInAt() != null);
            if (returnedEarlier) {
                log.info("Return {} for employee {} absorbed: already back", request.eventId(), employee.getId());
                return open;
            }
            throw new InvalidOperationException("Employee is not out on a break");
        }

        // Never earlier than when they left (device clock skew).
        out.setCheckedInAt(eventTime.isBefore(out.getCheckedOutAt()) ? out.getCheckedOutAt() : eventTime);
        out.setReturnEventId(request.eventId());
        checkoutRepository.save(out);

        return open;
    }

    private Attendance openSession(Employee employee, LocalDateTime eventTime, String whenNone) {
        return attendanceRepository
                .findFirstByEmployeeIdAndAttendanceDateAndClockOutIsNullOrderByClockInDesc(
                        employee.getId(), eventTime.toLocalDate())
                .or(() -> attendanceRepository
                        .findFirstByEmployeeIdAndClockOutIsNullOrderByClockInDesc(employee.getId())
                        .filter(a -> isRecentOpenSession(a, eventTime)))
                .orElseThrow(() -> new InvalidOperationException(whenNone));
    }

    private Attendance handleClockOut(Employee employee, LocalDateTime eventTime, BiometricSyncRequest request) {
        // Checked out already today (the other PC recorded it first): the day is closed, nothing more to do.
        var openToday = attendanceRepository.findFirstByEmployeeIdAndAttendanceDateAndClockOutIsNullOrderByClockInDesc(
                employee.getId(), eventTime.toLocalDate());
        if (openToday.isEmpty()) {
            var closedToday = attendanceRepository
                    .findByEmployeeIdAndAttendanceDateBetween(employee.getId(), eventTime.toLocalDate(), eventTime.toLocalDate())
                    .stream()
                    .filter(a -> a.getClockOut() != null)
                    .max(java.util.Comparator.comparing(Attendance::getClockOut));
            boolean anyOpenRecently = attendanceRepository.findFirstByEmployeeIdAndClockOutIsNullOrderByClockInDesc(employee.getId())
                    .filter(a -> isRecentOpenSession(a, eventTime))
                    .isPresent();
            if (closedToday.isPresent() && !anyOpenRecently) {
                log.info("Check-out {} for employee {} absorbed: already checked out", request.eventId(), employee.getId());
                return closedToday.get();
            }
        }

        Attendance open = openSession(employee, eventTime, "No open clock-in found for this employee to close");

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