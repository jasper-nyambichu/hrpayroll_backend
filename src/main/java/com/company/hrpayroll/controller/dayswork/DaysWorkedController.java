// controller/dayswork/DaysWorkedController.java
package com.company.hrpayroll.controller.dayswork;

import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.dayswork.DaysWorkedResponse;
import com.company.hrpayroll.service.dayswork.DaysWorkedService;
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

    // The Back Office produces the figures of its own branch; Payroll reads them. Admin inherits Super Admin's access.
    private static final String DAYS_READ_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER') or (hasRole('BACK_OFFICE') and @accessGuard.isSameBranch(#branchId))";

    // One row per active employee of the branch, counted live from attendance. Nothing is locked any more:
    // the agent payment calculation reads the same count whenever the Admin runs it.
    @GetMapping
    @PreAuthorize(DAYS_READ_ROLES)
    public ApiResponse<List<DaysWorkedResponse>> getForPeriod(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd
    ) {
        return ApiResponse.success(daysWorkedService.getForPeriod(branchId, periodStart, periodEnd));
    }
}
