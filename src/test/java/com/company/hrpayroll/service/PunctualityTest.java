package com.company.hrpayroll.service;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.attendance.AttendanceCheckout;
import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.offday.OffDay;
import com.company.hrpayroll.repository.attendance.AttendanceCheckoutRepository;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.company.CompanySettingsRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.offday.OffDayRepository;
import com.company.hrpayroll.service.attendance.impl.PunctualityService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The attendance record: days reported, arrivals against the work start, early leaving, late lunch returns. */
class PunctualityTest {

    private final AttendanceRepository attendance = mock(AttendanceRepository.class);
    private final AttendanceCheckoutRepository checkouts = mock(AttendanceCheckoutRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final OffDayRepository offDays = mock(OffDayRepository.class);
    private final CompanySettingsRepository settings = mock(CompanySettingsRepository.class);
    private final BusinessClock clock = mock(BusinessClock.class);
    private final PunctualityService service = new PunctualityService(attendance, checkouts, employees,
            mock(BranchRepository.class), offDays, settings, clock);

    private static LocalDateTime at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 11, day, hour, minute);
    }

    private static Attendance day(long id, int day, LocalDateTime in, LocalDateTime out) {
        Attendance a = new Attendance();
        a.setId(id);
        a.setEmployeeId(5L);
        a.setBranchId(1L);
        a.setAttendanceDate(LocalDate.of(2026, 11, day));
        a.setClockIn(in);
        a.setClockOut(out);
        return a;
    }

    @Test
    void aWeekIsCountedAgainstTheWorkTimes() {
        Employee e = new Employee();
        e.setId(5L);
        e.setFullName("Jane Wanjiru");
        e.setBranchId(1L);
        e.setEmploymentStatus("ACTIVE");
        when(employees.findById(5L)).thenReturn(Optional.of(e));
        when(clock.today()).thenReturn(LocalDate.of(2026, 11, 5));
        CompanySettings s = new CompanySettings();
        s.setWorkStartTime(LocalTime.of(8, 0));
        s.setWorkEndTime(LocalTime.of(17, 0));
        when(settings.findAll()).thenReturn(List.of(s));

        // 1st on time, 2nd 15 minutes late and left an hour early, 3rd off day, 4th missed, 5th early (still in).
        when(attendance.findByEmployeeIdInAndAttendanceDateBetween(any(), any(), any())).thenReturn(List.of(
                day(1, 1, at(1, 7, 55), at(1, 17, 5)),
                day(2, 2, at(2, 8, 15), at(2, 16, 0)),
                day(5, 5, at(5, 7, 40), null)));
        AttendanceCheckout lunch = new AttendanceCheckout();
        lunch.setAttendanceId(2L);
        lunch.setCheckType(AttendanceCheckout.LUNCH);
        lunch.setCheckedOutAt(at(2, 13, 0));
        lunch.setExpectedBackAt(at(2, 13, 30));
        lunch.setCheckedInAt(at(2, 13, 42));
        AttendanceCheckout earlyBack = new AttendanceCheckout();
        earlyBack.setAttendanceId(1L);
        earlyBack.setCheckType(AttendanceCheckout.LUNCH);
        earlyBack.setCheckedOutAt(at(1, 13, 0));
        earlyBack.setExpectedBackAt(at(1, 13, 30));
        earlyBack.setCheckedInAt(at(1, 13, 10));
        when(checkouts.findByAttendanceIdInOrderByCheckedOutAtAsc(any())).thenReturn(List.of(earlyBack, lunch));
        OffDay off = new OffDay();
        off.setEmployeeId(5L);
        off.setStartDate(LocalDate.of(2026, 11, 3));
        off.setEndDate(LocalDate.of(2026, 11, 3));
        when(offDays.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(any(), any())).thenReturn(List.of(off));

        var report = service.report(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, 5L);
        var row = report.employees().get(0);

        assertEquals(4, row.expectedDays());       // 1, 2, 4, 5 (the 3rd was off; nothing after today)
        assertEquals(3, row.daysReported());
        assertEquals(1, row.daysOff());
        assertEquals(1, row.daysMissed());
        assertEquals(2, row.onTimeArrivals());
        assertEquals(1, row.lateArrivals());
        assertEquals(15, row.minutesLateArrival());
        assertEquals(1, row.earlyLeaves());
        assertEquals(60, row.minutesLeftEarly());
        assertEquals(2, row.lunches());
        assertEquals(1, row.lateLunchReturns());   // back 20 minutes early is fine; 12 minutes late counts
        assertEquals(12, row.minutesLateLunch());
        assertEquals(75, row.attendancePercent());
        assertEquals(66, row.punctualityPercent());
        assertEquals(5, row.days().size());
        assertEquals(LocalDate.of(2026, 11, 5), row.days().get(0).date());
        assertNull(row.days().get(0).minutesLate());
    }
}
