package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.response.PageResponse;
import com.company.hrpayroll.dto.response.attendance.AttendanceDashboardDtos.*;
import com.company.hrpayroll.entity.attendance.Attendance;
import com.company.hrpayroll.entity.attendance.AttendanceCheckout;
import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.entity.attendance.BiometricSyncFailure;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.branch.Branch;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.offday.OffDay;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.repository.attendance.AttendanceCheckoutRepository;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.attendance.BiometricSyncFailureRepository;
import com.company.hrpayroll.repository.branch.BranchRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.offday.OffDayRepository;
import com.company.hrpayroll.service.attendance.AttendanceDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceDashboardServiceImpl implements AttendanceDashboardService {

    private static final Duration DEVICE_ONLINE_WINDOW = Duration.ofMinutes(5);
    private static final int MAX_RANGE_DAYS = 366;

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final BiometricDeviceRepository deviceRepository;
    private final BiometricSyncFailureRepository failureRepository;
    private final OffDayRepository offDayRepository;
    private final AttendanceCheckoutRepository checkoutRepository;
    private final BusinessClock businessClock;

    // ------------------------------------------------------------------ summary

    @Override
    @Transactional(readOnly = true)
    public DashboardSummary summary(LocalDate date, Long branchId) {
        LocalDate day = date != null ? date : businessClock.today();
        Long scope = scopeBranch(branchId);
        LocalDateTime now = businessClock.now();

        List<Branch> branches = new ArrayList<>();
        if (scope == null) branches.addAll(branchRepository.findByStatusNotOrderByCodeAsc("DELETED"));
        else branchRepository.findById(scope).ifPresent(branches::add);
        branches.removeIf(b -> !"ACTIVE".equals(b.getStatus()));
        branches.sort(Comparator.comparing(Branch::getName, String.CASE_INSENSITIVE_ORDER));

        Set<Long> branchIds = branches.stream().map(Branch::getId).collect(Collectors.toSet());

        Map<Long, List<Employee>> employeesByBranch = employeeRepository.findByEmploymentStatus("ACTIVE").stream()
                .filter(e -> branchIds.contains(e.getBranchId()))
                .collect(Collectors.groupingBy(Employee::getBranchId));

        Map<Long, List<Attendance>> recordsByEmployee = attendanceRepository.findByAttendanceDate(day).stream()
                .collect(Collectors.groupingBy(Attendance::getEmployeeId));

        Set<Long> onOffDay = offDayRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(day, day).stream()
                .map(OffDay::getEmployeeId)
                .collect(Collectors.toSet());

        Map<Long, List<BiometricDevice>> devicesByBranch = deviceRepository.findAll().stream()
                .filter(d -> "ACTIVE".equals(d.getStatus()))
                .collect(Collectors.groupingBy(BiometricDevice::getBranchId));

        List<BranchSummary> summaries = new ArrayList<>();
        int totalExpected = 0, totalReported = 0, totalIn = 0, totalOut = 0, totalOff = 0, devicesTotal = 0, devicesOnline = 0;

        for (Branch branch : branches) {
            List<Employee> branchEmployees = employeesByBranch.getOrDefault(branch.getId(), List.of());
            int in = 0, out = 0, off = 0;
            LocalDateTime lastActivity = null;

            for (Employee employee : branchEmployees) {
                List<Attendance> records = recordsByEmployee.getOrDefault(employee.getId(), List.of());
                if (records.isEmpty()) {
                    if (onOffDay.contains(employee.getId())) off++;
                    continue;
                }

                if (records.stream().anyMatch(a -> a.getClockOut() == null)) in++;
                else out++;

                for (Attendance a : records) {
                    lastActivity = latest(lastActivity, a.getClockIn());
                    lastActivity = latest(lastActivity, a.getClockOut());
                }
            }

            // Someone on an off day is not expected, unless they scanned anyway.
            int expected = branchEmployees.size() - off;
            int reported = in + out;

            List<BiometricDevice> devices = devicesByBranch.getOrDefault(branch.getId(), List.of());
            LocalDateTime deviceLast = devices.stream().map(BiometricDevice::getLastSyncAt)
                    .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
            int onlineDevices = (int) devices.stream().filter(d -> isOnline(d, now)).count();
            boolean anyScanning = devices.stream().anyMatch(d -> isScanning(d, now));
            String deviceStatus = devices.isEmpty() ? "NO_DEVICE"
                    : anyScanning ? "ONLINE"
                    : onlineDevices > 0 ? "READER_DISCONNECTED"
                    : "OFFLINE";

            summaries.add(new BranchSummary(
                    branch.getId(), branch.getName(), branch.getCode(),
                    expected, reported, in, out, expected - reported, off, percent(reported, expected),
                    lastActivity, deviceStatus, deviceLast));

            totalExpected += expected;
            totalReported += reported;
            totalIn += in;
            totalOut += out;
            totalOff += off;
            devicesTotal += devices.size();
            devicesOnline += onlineDevices;
        }

        return new DashboardSummary(day, now, totalExpected, totalReported, totalIn, totalOut,
                totalExpected - totalReported, totalOff, percent(totalReported, totalExpected),
                devicesTotal, devicesOnline, summaries);
    }

    // ------------------------------------------------------------------ live feed

    @Override
    @Transactional(readOnly = true)
    public List<ActivityItem> activity(Long branchId, LocalDateTime since, int limit) {
        Long scope = scopeBranch(branchId);
        LocalDateTime from = since != null ? since : businessClock.today().atStartOfDay();
        int cap = Math.min(Math.max(limit, 1), 200);

        List<Attendance> rows = attendanceRepository.findActivitySince(from).stream()
                .filter(a -> scope == null || scope.equals(a.getBranchId()))
                .toList();

        Map<Long, Employee> employees = employeesById(rows.stream().map(Attendance::getEmployeeId).collect(Collectors.toSet()));
        Map<Long, Branch> branches = branchesById(rows.stream().map(Attendance::getBranchId).collect(Collectors.toSet()));

        List<ActivityItem> items = new ArrayList<>();
        for (Attendance a : rows) {
            if (a.getClockIn() != null && !a.getClockIn().isBefore(from))
                items.add(activityItem(a, "CLOCK_IN", a.getClockIn(), employees, branches));
            if (a.getClockOut() != null && !a.getClockOut().isBefore(from))
                items.add(activityItem(a, "CLOCK_OUT", a.getClockOut(), employees, branches));
        }

        items.sort(Comparator.comparing(ActivityItem::occurredAt, Comparator.reverseOrder()));
        return items.size() > cap ? new ArrayList<>(items.subList(0, cap)) : items;
    }

    // ------------------------------------------------------------------ records table

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecordItem> records(Long branchId, LocalDate start, LocalDate end,
                                            boolean openOnly, int page, int size) {
        Long scope = scopeBranch(branchId);
        LocalDate to = end != null ? end : businessClock.today();
        LocalDate from = start != null ? start : to;

        if (from.isAfter(to)) throw new InvalidOperationException("Start date must not be after end date");
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS)
            throw new InvalidOperationException("Date range cannot exceed " + MAX_RANGE_DAYS + " days");

        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "attendanceDate", "clockIn"));

        Page<Attendance> result;
        if (scope == null) {
            result = openOnly
                    ? attendanceRepository.findByAttendanceDateBetweenAndClockOutIsNull(from, to, pageable)
                    : attendanceRepository.findByAttendanceDateBetween(from, to, pageable);
        } else {
            result = openOnly
                    ? attendanceRepository.findByBranchIdAndAttendanceDateBetweenAndClockOutIsNull(scope, from, to, pageable)
                    : attendanceRepository.findByBranchIdAndAttendanceDateBetween(scope, from, to, pageable);
        }

        Map<Long, Employee> employees = employeesById(
                result.getContent().stream().map(Attendance::getEmployeeId).collect(Collectors.toSet()));
        Map<Long, Branch> branches = branchesById(
                result.getContent().stream().map(Attendance::getBranchId).collect(Collectors.toSet()));

        Map<Long, List<CheckoutItem>> checkouts = checkoutRepository
                .findByAttendanceIdInOrderByCheckedOutAtAsc(result.getContent().stream().map(Attendance::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(AttendanceCheckout::getAttendanceId,
                        Collectors.mapping(AttendanceDashboardServiceImpl::checkoutItem, Collectors.toList())));

        return PageResponse.from(result.map(a -> recordItem(a, employees, branches, checkouts.getOrDefault(a.getId(), List.of()))));
    }

    // ------------------------------------------------------------------ devices / failures

    @Override
    @Transactional(readOnly = true)
    public List<DeviceItem> devices(Long branchId) {
        Long scope = scopeBranch(branchId);
        LocalDateTime now = businessClock.now();

        List<BiometricDevice> devices = deviceRepository.findAll().stream()
                .filter(d -> scope == null || scope.equals(d.getBranchId()))
                .toList();
        Map<Long, Branch> branches = branchesById(devices.stream().map(BiometricDevice::getBranchId).collect(Collectors.toSet()));

        return devices.stream()
                .map(d -> new DeviceItem(
                        d.getId(), d.getBranchId(),
                        Optional.ofNullable(branches.get(d.getBranchId())).map(Branch::getName).orElse(null),
                        d.getDeviceSerial(), d.getStatus(), d.getLastSyncAt(), isOnline(d, now),
                        d.getLastSeenAt(), d.getReaderConnected(), d.getReaderChangedAt(), d.getAppVersion(), d.getPendingScans()))
                .sorted(Comparator.comparing(DeviceItem::branchName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SyncFailureItem> failures() {
        Long scope = scopeBranch(null);
        Set<String> serials = scope == null ? Set.of() : deviceRepository.findByBranchId(scope).stream()
                .map(BiometricDevice::getDeviceSerial)
                .collect(Collectors.toSet());
        List<BiometricSyncFailure> rows = failureRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .filter(f -> scope == null || (f.getDeviceSerial() != null && serials.contains(f.getDeviceSerial())))
                .toList();
        Map<Long, Employee> employees = employeesById(rows.stream()
                .map(BiometricSyncFailure::getEmployeeId).filter(Objects::nonNull).collect(Collectors.toSet()));

        return rows.stream()
                .map(f -> new SyncFailureItem(
                        f.getId(), f.getCreatedAt(), f.getEventId(), f.getDeviceSerial(), f.getEmployeeId(),
                        f.getEmployeeId() == null ? null
                                : Optional.ofNullable(employees.get(f.getEmployeeId())).map(Employee::getFullName).orElse(null),
                        f.getReason()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AgentToday agentToday(Long agentId) {
        if (agentId == null) {
            return new AgentToday(0, 0, 0);
        }

        List<Long> employeeIds = employeeRepository.findByAgentId(agentId).stream()
                .filter(employee -> "ACTIVE".equals(employee.getEmploymentStatus()))
                .map(Employee::getId)
                .toList();
        if (employeeIds.isEmpty()) {
            return new AgentToday(0, 0, 0);
        }

        LocalDate today = businessClock.today();
        List<Attendance> records = attendanceRepository.findByEmployeeIdInAndAttendanceDateBetween(employeeIds, today, today);

        int reported = (int) records.stream().filter(a -> a.getClockIn() != null).map(Attendance::getEmployeeId).distinct().count();
        int currentlyIn = (int) records.stream()
                .filter(a -> a.getClockIn() != null && a.getClockOut() == null)
                .map(Attendance::getEmployeeId)
                .distinct()
                .count();

        return new AgentToday(employeeIds.size(), reported, currentlyIn);
    }

    // ------------------------------------------------------------------ helpers

    // A Branch Manager or Back Office login only ever sees its own branch, whatever branchId it passes.
    private Long scopeBranch(Long requested) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user
                && (user.getRole() == Role.BRANCH_MANAGER || user.getRole() == Role.BACK_OFFICE)) {
            if (user.getBranchId() == null) {
                throw new AccessDeniedException("Your account is not assigned to a branch");
            }
            return user.getBranchId();
        }
        return requested;
    }

    // Its PC reported recently (or, for an older app that never reports, a scan arrived recently).
    private static boolean isOnline(BiometricDevice device, LocalDateTime now) {
        LocalDateTime last = latest(device.getLastSeenAt(), device.getLastSyncAt());
        return "ACTIVE".equals(device.getStatus())
                && last != null
                && Duration.between(last, now).compareTo(DEVICE_ONLINE_WINDOW) <= 0;
    }

    // Online and able to scan: a PC that reports its reader unplugged can't take fingerprints.
    private static boolean isScanning(BiometricDevice device, LocalDateTime now) {
        return isOnline(device, now) && !Boolean.FALSE.equals(device.getReaderConnected());
    }

    private static int percent(int part, int whole) {
        return whole == 0 ? 0 : Math.min(100, part * 100 / whole);
    }

    private static LocalDateTime latest(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private Map<Long, Employee> employeesById(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return employeeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
    }

    private Map<Long, Branch> branchesById(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return branchRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Branch::getId, Function.identity()));
    }

    private ActivityItem activityItem(Attendance a, String type, LocalDateTime at,
                                      Map<Long, Employee> employees, Map<Long, Branch> branches) {
        Employee e = employees.get(a.getEmployeeId());
        Branch b = branches.get(a.getBranchId());
        return new ActivityItem(
                a.getId(), a.getEmployeeId(),
                e != null ? e.getFullName() : null,
                e != null ? e.getEmployeeNumber() : null,
                a.getBranchId(), b != null ? b.getName() : null,
                type, at, a.getSource().name());
    }

    private static CheckoutItem checkoutItem(AttendanceCheckout c) {
        String returnStatus;
        Integer minutesLate = null;
        if (c.getCheckedInAt() == null) {
            returnStatus = "OUT";
        } else if (c.getExpectedBackAt() == null || !AttendanceCheckout.LUNCH.equals(c.getCheckType())) {
            // Only a lunch has a limit. A temporary check-out's return time is what they said, never lateness.
            returnStatus = "RETURNED";
        } else if (c.getCheckedInAt().isAfter(c.getExpectedBackAt())) {
            returnStatus = "LATE";
            // Rounded up, so being back a few seconds late never reads as 0 minutes.
            minutesLate = (int) Math.ceil(Duration.between(c.getExpectedBackAt(), c.getCheckedInAt()).toSeconds() / 60.0);
        } else {
            returnStatus = "ON_TIME";
        }

        return new CheckoutItem(c.getId(), c.getCheckType(), c.getCheckedOutAt(), c.getExpectedBackAt(), c.getCheckedInAt(),
                returnStatus, minutesLate, c.getReason());
    }

    private RecordItem recordItem(Attendance a, Map<Long, Employee> employees, Map<Long, Branch> branches, List<CheckoutItem> checkouts) {
        Employee e = employees.get(a.getEmployeeId());
        Branch b = branches.get(a.getBranchId());

        Double hours = null;
        if (a.getClockIn() != null && a.getClockOut() != null) {
            long minutes = Duration.between(a.getClockIn(), a.getClockOut()).toMinutes();
            hours = Math.round(minutes / 60.0 * 100.0) / 100.0;
        }

        return new RecordItem(
                a.getId(), a.getEmployeeId(),
                e != null ? e.getFullName() : null,
                e != null ? e.getEmployeeNumber() : null,
                e != null ? e.getPosition() : null,
                a.getBranchId(), b != null ? b.getName() : null,
                a.getAttendanceDate(), a.getClockIn(), a.getClockOut(), hours,
                a.getClockOut() == null ? "IN_PROGRESS" : "COMPLETED",
                a.getSource().name(),
                checkouts);
    }
}