package com.company.hrpayroll.service;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.attendance.BiometricSyncRequest;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.attendance.AttendanceCheckout;
import com.company.hrpayroll.entity.attendance.AttendanceSource;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.repository.attendance.AttendanceCheckoutRepository;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.leave.LeaveRequestRepository;
import com.company.hrpayroll.service.attendance.impl.BiometricSyncFailureLogger;
import com.company.hrpayroll.service.attendance.impl.BiometricSyncServiceImpl;
import com.company.hrpayroll.service.attendance.impl.TerminalIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** After clocking in, a scan takes someone to lunch or out temporarily. Only a lunch can make them late. */
class LunchAndTemporaryCheckoutTest {

    private static final ZoneId NAIROBI = ZoneId.of("Africa/Nairobi");
    private static final LocalDateTime NOON = LocalDateTime.of(2026, 11, 5, 12, 0);

    private final AttendanceRepository attendance = mock(AttendanceRepository.class);
    private final AttendanceCheckoutRepository checkouts = mock(AttendanceCheckoutRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final BiometricDeviceRepository devices = mock(BiometricDeviceRepository.class);
    private final TerminalIdentity identity = mock(TerminalIdentity.class);
    private final LeaveRequestRepository leave = mock(LeaveRequestRepository.class);
    private final BusinessClock clock = mock(BusinessClock.class);

    private final BiometricSyncServiceImpl service = new BiometricSyncServiceImpl(attendance, checkouts, employees, devices,
            identity, leave, mock(BiometricSyncFailureLogger.class), clock);

    private final Attendance shift = Attendance.builder().employeeId(1L).branchId(7L).attendanceDate(NOON.toLocalDate())
            .clockIn(NOON.minusHours(4)).source(AttendanceSource.BIOMETRIC).status("PRESENT").build();

    private static Instant at(LocalDateTime time) {
        return time.atZone(NAIROBI).toInstant();
    }

    private BiometricSyncRequest scan(String id, String type, LocalDateTime when, LocalDateTime back, String reason) {
        return new BiometricSyncRequest(id, "emp-1", "DEV-1", when, "FINGERPRINT", type, at(when),
                back == null ? null : at(back), reason);
    }

    @BeforeEach
    void anEmployeeIsOnShift() {
        shift.setId(50L);

        BiometricDevice device = BiometricDevice.builder().branchId(7L).deviceSerial("DEV-1").build();
        device.setId(9L);
        when(devices.findByDeviceSerial("DEV-1")).thenReturn(Optional.of(device));
        when(identity.employeeFor(9L, "emp-1")).thenReturn(Optional.of(1L));

        Employee employee = Employee.builder().fullName("Alice").branchId(7L).build();
        employee.setId(1L);
        employee.setEmploymentStatus("ACTIVE");
        when(employees.findById(1L)).thenReturn(Optional.of(employee));

        when(clock.resolve(any(), any())).thenAnswer(call -> LocalDateTime.ofInstant(call.<Instant>getArgument(1), NAIROBI));
        when(clock.now()).thenReturn(NOON);
        when(leave.findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(eq(1L), any(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(attendance.findFirstByEmployeeIdAndAttendanceDateAndClockOutIsNullOrderByClockInDesc(eq(1L), any()))
                .thenReturn(Optional.of(shift));
        when(checkouts.findByEventId(any())).thenReturn(Optional.empty());
        when(checkouts.findByReturnEventId(any())).thenReturn(Optional.empty());
        when(attendance.findByEventId(any())).thenReturn(Optional.empty());
        when(attendance.findByClockOutEventId(any())).thenReturn(Optional.empty());
    }

    private AttendanceCheckout saved() {
        ArgumentCaptor<AttendanceCheckout> row = ArgumentCaptor.forClass(AttendanceCheckout.class);
        verify(checkouts).save(row.capture());
        return row.getValue();
    }

    @Test
    void lunchAllowsThirtyMinutes() {
        service.syncEvent(scan("e1", "LUNCH_OUT", NOON, null, null));

        AttendanceCheckout row = saved();
        assertEquals(AttendanceCheckout.LUNCH, row.getCheckType());
        assertEquals(NOON, row.getCheckedOutAt());
        assertEquals(NOON.plusMinutes(30), row.getExpectedBackAt());
        assertEquals("e1", row.getEventId());
    }

    @Test
    void aTemporaryCheckoutKeepsTheReasonAndReturnTime() {
        service.syncEvent(scan("e2", "EMERGENCY_OUT", NOON, NOON.plusHours(2), "  Sick child  "));

        AttendanceCheckout row = saved();
        assertEquals(AttendanceCheckout.EMERGENCY, row.getCheckType());
        assertEquals("Sick child", row.getReason());
        assertEquals(NOON.plusHours(2), row.getExpectedBackAt());
    }

    @Test
    void aTemporaryCheckoutNeedsAReason() {
        assertThrows(InvalidOperationException.class,
                () -> service.syncEvent(scan("e3", "EMERGENCY_OUT", NOON, NOON.plusHours(2), "   ")));
        verify(checkouts, never()).save(any());
    }

    @Test
    void aTemporaryCheckoutNeedsAReturnTimeLaterThatDay() {
        assertThrows(InvalidOperationException.class,
                () -> service.syncEvent(scan("e4", "EMERGENCY_OUT", NOON, null, "Bank")));
        assertThrows(InvalidOperationException.class,
                () -> service.syncEvent(scan("e5", "EMERGENCY_OUT", NOON, NOON.minusMinutes(5), "Bank")));
        assertThrows(InvalidOperationException.class,
                () -> service.syncEvent(scan("e6", "EMERGENCY_OUT", NOON, NOON.plusDays(1), "Bank")));
        verify(checkouts, never()).save(any());
    }

    @Test
    void comingBackClosesTheBreak() {
        AttendanceCheckout out = AttendanceCheckout.builder().attendanceId(50L).checkType(AttendanceCheckout.LUNCH)
                .checkedOutAt(NOON).expectedBackAt(NOON.plusMinutes(30)).build();
        when(checkouts.findFirstByAttendanceIdAndCheckedInAtIsNullOrderByCheckedOutAtDesc(50L)).thenReturn(Optional.of(out));

        service.syncEvent(scan("e7", "BREAK_RETURN", NOON.plusMinutes(42), null, null));

        assertEquals(NOON.plusMinutes(42), out.getCheckedInAt());
        assertEquals("e7", out.getReturnEventId());
        assertNull(shift.getClockOut());
    }

    @Test
    void someoneAlreadyOutCannotLeaveAgain() {
        when(checkouts.findFirstByAttendanceIdAndCheckedInAtIsNullOrderByCheckedOutAtDesc(50L))
                .thenReturn(Optional.of(AttendanceCheckout.builder().attendanceId(50L).checkedOutAt(NOON).build()));

        assertThrows(InvalidOperationException.class, () -> service.syncEvent(scan("e8", "LUNCH_OUT", NOON.plusMinutes(1), null, null)));
    }

    @Test
    void noBreakToReturnFromIsRefused() {
        when(checkouts.findFirstByAttendanceIdAndCheckedInAtIsNullOrderByCheckedOutAtDesc(50L)).thenReturn(Optional.empty());

        assertThrows(InvalidOperationException.class, () -> service.syncEvent(scan("e9", "BREAK_RETURN", NOON, null, null)));
    }

    @Test
    void aRetriedScanIsNotRecordedTwice() {
        when(checkouts.findByEventId("e1")).thenReturn(Optional.of(
                AttendanceCheckout.builder().attendanceId(50L).checkedOutAt(NOON).build()));
        when(attendance.findById(50L)).thenReturn(Optional.of(shift));

        service.syncEvent(scan("e1", "LUNCH_OUT", NOON, null, null));

        verify(checkouts, never()).save(any());
    }
}
