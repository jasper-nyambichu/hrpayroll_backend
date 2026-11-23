package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.response.attendance.PunctualityDtos.DayLine;
import com.company.hrpayroll.dto.response.attendance.PunctualityDtos.EmployeePunctuality;
import com.company.hrpayroll.dto.response.attendance.PunctualityDtos.LateLunch;
import com.company.hrpayroll.dto.response.attendance.PunctualityDtos.PunctualityReport;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.attendance.AttendanceCheckout;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.offday.OffDay;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.repository.attendance.AttendanceCheckoutRepository;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.company.CompanySettingsRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.offday.OffDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The attendance record of each employee over a period: how often they reported, whether they arrived by the work
 * start time (Settings), left before the work end, and came back from lunch on time.
 *
 * Expected days are the calendar days of the period up to today, from the day they were hired, less their off days
 * (an employee has two a month). Several clock-ins on one day count as one day: the first arrival and the last
 * departure decide it.
 */
@Service
@RequiredArgsConstructor
public class PunctualityService {

    private static final int MAX_RANGE_DAYS = 92;
    private static final LocalTime DEFAULT_START = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_END = LocalTime.of(17, 0);

    private final AttendanceRepository attendanceRepository;
    private final AttendanceCheckoutRepository checkoutRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final OffDayRepository offDayRepository;
    private final CompanySettingsRepository settingsRepository;
    private final BusinessClock businessClock;
    private final com.company.hrpayroll.service.company.OrganisationOperations operations;

    @Transactional(readOnly = true)
    public PunctualityReport report(LocalDate start, LocalDate end, Long branchId, Long employeeId) {
        LocalDate today = businessClock.today();
        LocalDate to = end != null ? end : today;
        LocalDate from = start != null ? start : to.withDayOfMonth(1);
        if (to.isBefore(from)) throw new InvalidOperationException("The end date cannot be before the start date");
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new InvalidOperationException("A period can cover at most " + MAX_RANGE_DAYS + " days");
        }

        Long scope = scopeBranch(branchId);
        List<Employee> employees;
        if (employeeId != null) {
            Employee one = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
            if (scope != null && !scope.equals(one.getBranchId())) {
                throw new AccessDeniedException("This employee works at another branch");
            }
            employees = List.of(one);
        } else {
            employees = (scope != null ? employeeRepository.findByBranchId(scope) : employeeRepository.findByEmploymentStatus("ACTIVE"))
                    .stream().filter(e -> "ACTIVE".equals(e.getEmploymentStatus())).toList();
        }

        // Each day is measured against its own hours (Settings → work week); a day the organisation does not work
        // is not expected. The report's single start/end are the first working day's, for the headings.
        Map<java.time.DayOfWeek, com.company.hrpayroll.service.company.OrganisationOperations.Day> week = operations.schedule();
        var firstWorking = week.values().stream().filter(com.company.hrpayroll.service.company.OrganisationOperations.Day::working).findFirst();
        LocalTime workStart = firstWorking.map(com.company.hrpayroll.service.company.OrganisationOperations.Day::startTime).orElse(DEFAULT_START);
        LocalTime workEnd = firstWorking.map(com.company.hrpayroll.service.company.OrganisationOperations.Day::endTime).orElse(DEFAULT_END);

        List<Long> ids = employees.stream().map(Employee::getId).toList();
        Map<Long, List<Attendance>> attendance = ids.isEmpty() ? Map.of()
                : attendanceRepository.findByEmployeeIdInAndAttendanceDateBetween(ids, from, to).stream()
                .collect(Collectors.groupingBy(Attendance::getEmployeeId));
        Map<Long, List<AttendanceCheckout>> checkoutsByAttendance = checkoutsFor(attendance.values().stream().flatMap(List::stream).toList());

        Set<Long> idSet = new HashSet<>(ids);
        Map<Long, Set<LocalDate>> offDates = new HashMap<>();
        for (OffDay off : offDayRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(to, from)) {
            if (!idSet.contains(off.getEmployeeId())) continue;
            for (LocalDate d = max(off.getStartDate(), from); !d.isAfter(min(off.getEndDate(), to)); d = d.plusDays(1)) {
                offDates.computeIfAbsent(off.getEmployeeId(), k -> new HashSet<>()).add(d);
            }
        }

        Map<Long, String> branchNames = branchRepository.findAllById(
                        employees.stream().map(Employee::getBranchId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Branch::getId, Branch::getName));

        List<EmployeePunctuality> rows = employees.stream()
                .map(e -> forEmployee(e, from, to, today, week,
                        attendance.getOrDefault(e.getId(), List.of()), checkoutsByAttendance,
                        offDates.getOrDefault(e.getId(), Set.of()), branchNames.get(e.getBranchId()), employeeId != null))
                .sorted(Comparator.comparing(EmployeePunctuality::employeeName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return new PunctualityReport(from, to, workStart, workEnd, rows);
    }

    /** Lunch returns after the 30 minutes on one day, and anyone still out past the end of lunch, latest first. */
    @Transactional(readOnly = true)
    public List<LateLunch> lateLunches(LocalDate date, Long branchId) {
        LocalDate day = date != null ? date : businessClock.today();
        LocalDateTime now = businessClock.now();
        Long scope = scopeBranch(branchId);

        List<Attendance> records = attendanceRepository.findByAttendanceDate(day).stream()
                .filter(a -> scope == null || scope.equals(a.getBranchId()))
                .toList();
        Map<Long, Attendance> byId = records.stream().collect(Collectors.toMap(Attendance::getId, Function.identity()));
        Map<Long, Employee> employees = employeeRepository.findAllById(
                records.stream().map(Attendance::getEmployeeId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, String> branchNames = branchRepository.findAllById(
                        records.stream().map(Attendance::getBranchId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Branch::getId, Branch::getName));

        List<LateLunch> late = new ArrayList<>();
        for (List<AttendanceCheckout> list : checkoutsFor(records).values()) {
            for (AttendanceCheckout c : list) {
                if (!AttendanceCheckout.LUNCH.equals(c.getCheckType()) || c.getExpectedBackAt() == null) continue;
                LocalDateTime back = c.getCheckedInAt() != null ? c.getCheckedInAt() : now;
                if (!back.isAfter(c.getExpectedBackAt())) continue;
                if (c.getCheckedInAt() == null && !day.equals(businessClock.today())) continue;   // never came back: not a lunch question
                Attendance a = byId.get(c.getAttendanceId());
                Employee e = employees.get(a.getEmployeeId());
                late.add(new LateLunch(a.getEmployeeId(), e != null ? e.getFullName() : null, e != null ? e.getEmployeeNumber() : null,
                        a.getBranchId(), branchNames.get(a.getBranchId()), day,
                        c.getCheckedOutAt(), c.getExpectedBackAt(), c.getCheckedInAt(), minutesLate(c.getExpectedBackAt(), back)));
            }
        }
        late.sort(Comparator.comparing(LateLunch::minutesLate).reversed());
        return late;
    }

    // ------------------------------------------------------------------------------------------------

    private EmployeePunctuality forEmployee(Employee e, LocalDate from, LocalDate to, LocalDate today,
                                            Map<java.time.DayOfWeek, com.company.hrpayroll.service.company.OrganisationOperations.Day> week,
                                            List<Attendance> records,
                                            Map<Long, List<AttendanceCheckout>> checkouts, Set<LocalDate> offDays,
                                            String branchName, boolean withDays) {
        Map<LocalDate, List<Attendance>> byDate = new TreeMap<>(records.stream()
                .filter(a -> a.getClockIn() != null)
                .collect(Collectors.groupingBy(Attendance::getAttendanceDate)));

        LocalDate firstDay = e.getDateHired() != null && e.getDateHired().isAfter(from) ? e.getDateHired() : from;
        LocalDate lastDay = to.isAfter(today) ? today : to;

        int expected = 0, off = 0, missed = 0;
        int onTime = 0, lateArrivals = 0, lateMinutes = 0, earlyLeaves = 0, earlyMinutes = 0;
        int lunches = 0, lateLunches = 0, lateLunchMinutes = 0;
        long workedMinutes = 0;
        List<DayLine> days = new ArrayList<>();

        for (LocalDate d = firstDay; !d.isAfter(lastDay); d = d.plusDays(1)) {
            List<Attendance> day = byDate.get(d);
            var hours = week.get(d.getDayOfWeek());
            // A day the organisation does not work is not expected; anyone who came anyway is not late or early.
            if (!hours.working() && day == null) continue;
            boolean closedDay = !hours.working();
            LocalTime workStart = hours.startTime();
            LocalTime workEnd = hours.endTime();
            boolean isOff = offDays.contains(d);
            if (isOff && day == null) {
                off++;
                if (withDays) days.add(new DayLine(d, true, null, null, null, null, null, null));
                continue;
            }
            expected++;
            if (day == null) {
                missed++;
                if (withDays) days.add(new DayLine(d, false, null, null, null, null, null, null));
                continue;
            }

            LocalDateTime in = day.stream().map(Attendance::getClockIn).min(Comparator.naturalOrder()).orElseThrow();
            boolean stillIn = day.stream().anyMatch(a -> a.getClockOut() == null);
            LocalDateTime out = stillIn ? null : day.stream().map(Attendance::getClockOut).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);

            Integer late = !closedDay && in.toLocalTime().isAfter(workStart) ? minutesLate(d.atTime(workStart), in) : null;
            if (late != null) { lateArrivals++; lateMinutes += late; } else onTime++;

            Integer early = !closedDay && out != null && out.toLocalDate().equals(d) && out.toLocalTime().isBefore(workEnd)
                    ? (int) Duration.between(out, d.atTime(workEnd)).toMinutes() : null;
            if (early != null && early > 0) { earlyLeaves++; earlyMinutes += early; } else early = null;

            long dayMinutes = day.stream().filter(a -> a.getClockOut() != null)
                    .mapToLong(a -> Duration.between(a.getClockIn(), a.getClockOut()).toMinutes()).sum();
            workedMinutes += dayMinutes;

            Integer lunchLate = null;
            for (Attendance a : day) {
                for (AttendanceCheckout c : checkouts.getOrDefault(a.getId(), List.of())) {
                    if (!AttendanceCheckout.LUNCH.equals(c.getCheckType())) continue;
                    lunches++;
                    if (c.getExpectedBackAt() != null && c.getCheckedInAt() != null && c.getCheckedInAt().isAfter(c.getExpectedBackAt())) {
                        int m = minutesLate(c.getExpectedBackAt(), c.getCheckedInAt());
                        lateLunches++;
                        lateLunchMinutes += m;
                        lunchLate = (lunchLate == null ? 0 : lunchLate) + m;
                    }
                }
            }

            if (withDays) {
                days.add(new DayLine(d, isOff, in, out, late, early, lunchLate,
                        dayMinutes > 0 ? Math.round(dayMinutes / 60.0 * 100.0) / 100.0 : null));
            }
        }

        int reported = byDate.keySet().stream().filter(d -> !d.isBefore(firstDay) && !d.isAfter(lastDay)).mapToInt(d -> 1).sum();
        Integer attendancePercent = expected == 0 ? null : Math.min(100, reported * 100 / expected);
        Integer punctualityPercent = reported == 0 ? null : onTime * 100 / reported;

        if (withDays) days.sort(Comparator.comparing(DayLine::date).reversed());

        return new EmployeePunctuality(
                e.getId(), e.getFullName(), e.getEmployeeNumber(), e.getPosition(), e.getBranchId(), branchName,
                expected, reported, off, missed,
                onTime, lateArrivals, lateMinutes, earlyLeaves, earlyMinutes,
                lunches, lateLunches, lateLunchMinutes,
                Math.round(workedMinutes / 60.0 * 10.0) / 10.0,
                attendancePercent, punctualityPercent,
                rating(attendancePercent, punctualityPercent, reported, lateLunches),
                withDays ? days : null);
    }

    // Both reporting and arriving on time matter; a late lunch return costs a little too.
    static String rating(Integer attendance, Integer punctuality, int reported, int lateLunches) {
        if (attendance == null || reported == 0) return "NO_DATA";
        int score = Math.min(attendance, punctuality == null ? 0 : punctuality)
                - Math.min(20, lateLunches * 100 / Math.max(reported, 1) / 2);
        if (score >= 90) return "EXCELLENT";
        if (score >= 75) return "GOOD";
        if (score >= 50) return "FAIR";
        return "POOR";
    }

    private Map<Long, List<AttendanceCheckout>> checkoutsFor(List<Attendance> records) {
        if (records.isEmpty()) return Map.of();
        return checkoutRepository.findByAttendanceIdInOrderByCheckedOutAtAsc(records.stream().map(Attendance::getId).toList())
                .stream().collect(Collectors.groupingBy(AttendanceCheckout::getAttendanceId));
    }

    private java.util.Optional<CompanySettings> settings() {
        return settingsRepository.findAll().stream().findFirst();
    }

    // Rounded up, so a few seconds late never reads as 0 minutes.
    private static int minutesLate(LocalDateTime due, LocalDateTime at) {
        return (int) Math.ceil(Duration.between(due, at).toSeconds() / 60.0);
    }

    private static LocalDate max(LocalDate a, LocalDate b) { return a.isAfter(b) ? a : b; }
    private static LocalDate min(LocalDate a, LocalDate b) { return a.isBefore(b) ? a : b; }

    // A Branch Manager or Back Office login only ever sees its own branch, whatever branchId it passes.
    private static Long scopeBranch(Long requested) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user
                && (user.getRole() == Role.BRANCH_MANAGER || user.getRole() == Role.BACK_OFFICE)) {
            if (user.getBranchId() == null) throw new AccessDeniedException("Your account is not assigned to a branch");
            return user.getBranchId();
        }
        return requested;
    }
}
