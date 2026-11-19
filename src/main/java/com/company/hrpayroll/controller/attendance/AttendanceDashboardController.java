package com.company.hrpayroll.controller.attendance;

import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.PageResponse;
import com.company.hrpayroll.dto.response.attendance.AttendanceDashboardDtos.*;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.attendance.AttendanceDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Read-only data for the Admin, Branch Manager and Back Office attendance dashboards.
// A BRANCH_MANAGER or BACK_OFFICE login is automatically limited to its own branch inside the service.
@RestController
@RequestMapping("/attendance/dashboard")
@RequiredArgsConstructor
public class AttendanceDashboardController {

    private static final String VIEW_ROLES =
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER','PAYROLL_OFFICER','AUDITOR','BRANCH_MANAGER','BACK_OFFICE')";
    private static final String ADMIN_ROLES = "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER','BACK_OFFICE')";

    private final AttendanceDashboardService dashboardService;
    private final AccessGuard accessGuard;
    private final com.company.hrpayroll.service.attendance.impl.PunctualityService punctualityService;

    // The figures on the agent's dashboard, for their own employees only.
    @GetMapping("/my-agent-today")
    @PreAuthorize("hasRole('AGENT')")
    public ApiResponse<AgentToday> myAgentToday() {
        return ApiResponse.success(dashboardService.agentToday(accessGuard.currentAgentId()));
    }

    @GetMapping("/summary")
    @PreAuthorize(VIEW_ROLES)
    public ApiResponse<DashboardSummary> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long branchId
    ) {
        return ApiResponse.success(dashboardService.summary(date, branchId));
    }

    @GetMapping("/activity")
    @PreAuthorize(VIEW_ROLES)
    public ApiResponse<List<ActivityItem>> activity(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.success(dashboardService.activity(branchId, since, limit));
    }

    @GetMapping("/records")
    @PreAuthorize(VIEW_ROLES)
    public ApiResponse<PageResponse<RecordItem>> records(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "false") boolean openOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return ApiResponse.success(dashboardService.records(branchId, start, end, openOnly, page, size));
    }

    // Each employee's attendance record over a period: days reported, arrivals against the work start, early
    // departures, late lunch returns. With employeeId, one employee with a line per day.
    @GetMapping("/punctuality")
    @PreAuthorize(VIEW_ROLES)
    public ApiResponse<com.company.hrpayroll.dto.response.attendance.PunctualityDtos.PunctualityReport> punctuality(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long employeeId
    ) {
        return ApiResponse.success(punctualityService.report(start, end, branchId, employeeId));
    }

    // Who came back late from lunch on a day (today by default), and anyone still out past the end of lunch.
    @GetMapping("/late-lunch")
    @PreAuthorize(VIEW_ROLES)
    public ApiResponse<List<com.company.hrpayroll.dto.response.attendance.PunctualityDtos.LateLunch>> lateLunch(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long branchId
    ) {
        return ApiResponse.success(punctualityService.lateLunches(date, branchId));
    }

    @GetMapping("/devices")
    @PreAuthorize(VIEW_ROLES)
    public ApiResponse<List<DeviceItem>> devices(@RequestParam(required = false) Long branchId) {
        return ApiResponse.success(dashboardService.devices(branchId));
    }

    @GetMapping("/sync-failures")
    @PreAuthorize(ADMIN_ROLES)
    public ApiResponse<List<SyncFailureItem>> failures() {
        return ApiResponse.success(dashboardService.failures());
    }
}