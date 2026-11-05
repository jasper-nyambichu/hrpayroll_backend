// controller/dayswork/DaysWorkedController.java
package com.company.hrpayroll.controller.dayswork;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.dayswork.DaysWorkedLockRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.dayswork.DaysWorkedResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.dayswork.DaysWorkedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/days-worked")
@RequiredArgsConstructor
public class DaysWorkedController {

    private final DaysWorkedService daysWorkedService;
    private final AccessGuard accessGuard;

    // The Back Office produces the figures of its own branch; Payroll reads them. Admin inherits Super Admin's access.
    private static final String DAYS_READ_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER') or (hasRole('BACK_OFFICE') and @accessGuard.isSameBranch(#branchId))";

    private static final String DAYS_WRITE_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('BACK_OFFICE') and @accessGuard.isSameBranch(#request.branchId()))";

    // One row per active employee of the branch: locked figure if the period is locked, otherwise live.
    @GetMapping
    @PreAuthorize(DAYS_READ_ROLES)
    public ApiResponse<List<DaysWorkedResponse>> getForPeriod(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd
    ) {
        return ApiResponse.success(daysWorkedService.getForPeriod(branchId, periodStart, periodEnd));
    }

    @PostMapping("/lock")
    @PreAuthorize(DAYS_WRITE_ROLES)
    @Auditable(action = "DAYS_WORKED_LOCKED", entityType = "DaysWorkedRecord")
    public ApiResponse<List<DaysWorkedResponse>> lockPeriod(@Valid @RequestBody DaysWorkedLockRequest request) {
        return ApiResponse.success("Days worked locked for payroll",
                daysWorkedService.lockPeriod(request, accessGuard.currentUserId()));
    }

    // Reopening a locked period is Admin's call — the Back Office cannot undo its own lock.
    @DeleteMapping("/lock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "DAYS_WORKED_UNLOCKED", entityType = "DaysWorkedRecord")
    public ApiResponse<Void> unlockPeriod(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd
    ) {
        daysWorkedService.unlockPeriod(branchId, periodStart, periodEnd);
        return ApiResponse.success("Days worked unlocked", null);
    }
}
