package com.company.hrpayroll.dto.response.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** How each employee reports to work and keeps to work time, over a period. */
public final class PunctualityDtos {

    private PunctualityDtos() {}

    public record PunctualityReport(
            LocalDate start,
            LocalDate end,
            LocalTime workStart,
            LocalTime workEnd,
            List<EmployeePunctuality> employees
    ) {}

    public record EmployeePunctuality(
            Long employeeId,
            String employeeName,
            String employeeNumber,
            String position,
            Long branchId,
            String branchName,
            int expectedDays,           // days in the period up to today, less their off days
            int daysReported,
            int daysOff,
            int daysMissed,             // expected but never clocked in
            int onTimeArrivals,
            int lateArrivals,
            int minutesLateArrival,     // total
            int earlyLeaves,
            int minutesLeftEarly,       // total
            int lunches,
            int lateLunchReturns,
            int minutesLateLunch,       // total
            double hoursWorked,
            Integer attendancePercent,  // reported of expected; null when nothing was expected yet
            Integer punctualityPercent, // on-time arrivals of days reported; null before any day
            String rating,              // EXCELLENT | GOOD | FAIR | POOR | NO_DATA
            List<DayLine> days          // only when one employee is asked for
    ) {}

    public record DayLine(
            LocalDate date,
            boolean offDay,
            LocalDateTime clockIn,
            LocalDateTime clockOut,
            Integer minutesLate,        // arrival after the work start
            Integer minutesLeftEarly,   // left before the work end
            Integer lunchMinutesLate,   // back from lunch after the 30 minutes
            Double hours
    ) {}

    /** Someone back late from lunch (or still out past the end of it). */
    public record LateLunch(
            Long employeeId,
            String employeeName,
            String employeeNumber,
            Long branchId,
            String branchName,
            LocalDate date,
            LocalDateTime checkedOutAt,
            LocalDateTime expectedBackAt,
            LocalDateTime checkedInAt,  // null: still out
            int minutesLate
    ) {}
}
