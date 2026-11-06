// controller/attendance/AttendanceController.java
package com.company.hrpayroll.controller.attendance;

import com.company.hrpayroll.dto.request.attendance.AttendanceRequest;
import com.company.hrpayroll.dto.request.attendance.ManualCheckInRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;
import com.company.hrpayroll.dto.response.attendance.ManualCheckInResult;
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

    // Checking someone in by hand is the Branch Manager's job, not the Back Office's.
    @PostMapping("/clock-in")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER') " +
            "or (hasRole('BRANCH_MANAGER') and @accessGuard.isEmployeeInMyBranch(#request.employeeId()))")
    public ApiResponse<AttendanceResponse> clockInManually(@Valid @RequestBody AttendanceRequest request) {
        return ApiResponse.success("Clocked in", attendanceService.clockInManually(request));
    }

    /**
     * The kiosk's manual list, for when the fingerprint reader fails: a Branch Manager ticks employees of
     * their own branch and they are checked in at the server's time. Only a Branch Manager may do this.
     */
    @PostMapping("/manual-check-in")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ApiResponse<List<ManualCheckInResult>> checkInAtMyBranch(@Valid @RequestBody ManualCheckInRequest request) {
        List<ManualCheckInResult> results = attendanceService.checkInAtMyBranch(request.employeeIds());
        long done = results.stream().filter(ManualCheckInResult::checkedIn).count();
        return ApiResponse.success(done + " of " + results.size() + " checked in", results);
    }

    @PutMapping("/{attendanceId}/clock-out")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER') " +
            "or (hasAnyRole('BRANCH_MANAGER', 'BACK_OFFICE') and @accessGuard.isAttendanceInMyBranch(#attendanceId))")
    public ApiResponse<AttendanceResponse> clockOutManually(
            @PathVariable Long attendanceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime clockOutTime
    ) {
        return ApiResponse.success("Clocked out", attendanceService.clockOutManually(attendanceId, clockOutTime));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'PAYROLL_OFFICER', 'AUDITOR') " +
            "or (hasAnyRole('BRANCH_MANAGER', 'BACK_OFFICE') and @accessGuard.isEmployeeInMyBranch(#employeeId)) " +
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