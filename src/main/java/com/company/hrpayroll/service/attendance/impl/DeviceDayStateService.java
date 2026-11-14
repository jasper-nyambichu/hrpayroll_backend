// service/attendance/impl/DeviceDayStateService.java
package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.response.attendance.DeviceDayEventResponse;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.attendance.AttendanceCheckout;
import com.company.hrpayroll.entity.attendance.AttendanceSource;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.repository.attendance.AttendanceCheckoutRepository;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Today's attendance at a PC's branch, step by step, for every PC of that branch. With it, an employee who
 * checked in on Back Office PC 1 can take lunch, a temporary check-out or check out on PC 2: PC 2 decides
 * from the same day as PC 1 (plus its own scans not sent yet), so nothing is duplicated and no step is missed.
 *
 * Read-only, scoped to the calling PC's own branch (taken from its authenticated serial, never a parameter).
 */
@Service
@RequiredArgsConstructor
public class DeviceDayStateService {

    private final BiometricDeviceRepository deviceRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceCheckoutRepository checkoutRepository;
    private final TerminalIdentity terminalIdentity;
    private final BusinessClock businessClock;

    @Transactional(readOnly = true)
    public List<DeviceDayEventResponse> todayForDevice(String deviceSerial) {
        BiometricDevice device = deviceRepository.findByDeviceSerial(deviceSerial)
                .filter(found -> "ACTIVE".equals(found.getStatus()))
                .orElseThrow(() -> new InvalidOperationException("This PC's reader is not active"));

        LocalDate today = businessClock.today();
        // Yesterday too, for a session still open past midnight; the PCs only look at today's steps.
        List<Attendance> sessions = attendanceRepository.findByBranchIdAndAttendanceDateBetween(device.getBranchId(), today.minusDays(1), today);
        if (sessions.isEmpty()) {
            return List.of();
        }

        Map<Long, String> terminalIds = terminalIdentity.idsAtBranchOf(device);
        List<AttendanceCheckout> checkouts = checkoutRepository.findByAttendanceIdInOrderByCheckedOutAtAsc(
                sessions.stream().map(Attendance::getId).toList());

        List<DeviceDayEventResponse> events = new ArrayList<>();
        for (Attendance session : sessions) {
            String terminalUserId = terminalIds.get(session.getEmployeeId());
            if (session.getClockIn() != null) {
                String id = session.getEventId() != null ? session.getEventId()
                        : session.getSource() == AttendanceSource.MANUAL ? "manual-" + session.getId()
                        : "srv-in-" + session.getId();
                events.add(event(session, terminalUserId, id, "CLOCK_IN", session.getClockIn(), null, null));
            }
            for (AttendanceCheckout c : checkouts) {
                if (!c.getAttendanceId().equals(session.getId())) continue;
                boolean lunch = AttendanceCheckout.LUNCH.equals(c.getCheckType());
                events.add(event(session, terminalUserId, c.getEventId() != null ? c.getEventId() : "srv-co-" + c.getId(),
                        lunch ? "LUNCH_OUT" : "EMERGENCY_OUT", c.getCheckedOutAt(), c.getExpectedBackAt(), lunch ? null : c.getReason()));
                if (c.getCheckedInAt() != null) {
                    events.add(event(session, terminalUserId, c.getReturnEventId() != null ? c.getReturnEventId() : "srv-ret-" + c.getId(),
                            "BREAK_RETURN", c.getCheckedInAt(), null, null));
                }
            }
            if (session.getClockOut() != null) {
                events.add(event(session, terminalUserId,
                        session.getClockOutEventId() != null ? session.getClockOutEventId() : "srv-out-" + session.getId(),
                        "CLOCK_OUT", session.getClockOut(), null, null));
            }
        }
        events.sort(Comparator.comparing(DeviceDayEventResponse::at));
        return events;
    }

    private DeviceDayEventResponse event(Attendance session, String terminalUserId, String eventId, String type,
                                         LocalDateTime at, LocalDateTime expectedBack, String reason) {
        return new DeviceDayEventResponse(session.getEmployeeId(), terminalUserId, eventId, type,
                toInstant(at), expectedBack == null ? null : toInstant(expectedBack), reason);
    }

    private Instant toInstant(LocalDateTime wallClock) {
        return wallClock.atZone(businessClock.zone()).toInstant();
    }
}
