// controller/attendance/AttendanceController.java
package com.company.hrpayroll.controller.attendance;

import com.company.hrpayroll.dto.request.attendance.AttendanceRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.attendance.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AccessGuard accessGuard;

    @PostMapping("/clock-in")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'BRANCH_MANAGER')")
    public ApiResponse<AttendanceResponse> clockInManually(@Valid @RequestBody AttendanceRequest request) {
        return ApiResponse.success("Clocked in", attendanceService.clockInManually(request));
    }

    @PutMapping("/{attendanceId}/clock-out")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'BRANCH_MANAGER')")
    public ApiResponse<AttendanceResponse> clockOutManually(
            @PathVariable Long attendanceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime clockOutTime
    ) {
        return ApiResponse.success("Clocked out", attendanceService.clockOutManually(attendanceId, clockOutTime));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'AUDITOR') " +
            "or (hasRole('BRANCH_MANAGER') and @accessGuard.isEmployeeInMyBranch(#employeeId)) " +
            "or @accessGuard.isSameEmployee(#employeeId)")
    public ApiResponse<List<AttendanceResponse>> getAttendanceForEmployee(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ApiResponse.success(attendanceService.getAttendanceForEmployee(employeeId, start, end));
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'AUDITOR') or @accessGuard.isSameBranch(#branchId)")
    public ApiResponse<List<AttendanceResponse>> getAttendanceForBranch(
            @PathVariable Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ApiResponse.success(attendanceService.getAttendanceForBranch(branchId, start, end));
    }

    // ESS convenience — matches EmployeeController.getMyProfile()'s pattern.
    @GetMapping("/my-attendance")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<List<AttendanceResponse>> getMyAttendance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ApiResponse.success(
                attendanceService.getAttendanceForEmployee(accessGuard.currentEmployeeId(), start, end));
    }
}