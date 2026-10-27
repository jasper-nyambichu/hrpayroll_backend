// controller/damage/DamageController.java
package com.company.hrpayroll.controller.damage;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.damage.DamageOverrideRequest;
import com.company.hrpayroll.dto.request.damage.DamageRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.damage.DamageResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.damage.DamageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/damages")
@RequiredArgsConstructor
public class DamageController {

    private final DamageService damageService;
    private final AccessGuard accessGuard;

    // Everyone who may READ damages: the Back Office records them, Payroll deducts them, and the
    // Accounts Office sees what came off a payslip. Admin inherits Super Admin's access.
    private static final String DAMAGE_READ_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BACK_OFFICE', 'PAYROLL_OFFICER', 'FINANCE_OFFICER')";

    // Recording, overriding and cancelling are the Back Office's job (Admin can step in).
    private static final String DAMAGE_WRITE_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'BACK_OFFICE')";

    @PostMapping
    @PreAuthorize(DAMAGE_WRITE_ROLES)
    @Auditable(action = "DAMAGE_RECORDED", entityType = "Damage")
    public ApiResponse<DamageResponse> recordDamage(@Valid @RequestBody DamageRequest request) {
        return ApiResponse.success("Damage recorded — awaiting the employee's fingerprint",
                damageService.recordDamage(request, accessGuard.currentUserId()));
    }

    // Approve without the employee's fingerprint. The reason is mandatory and permanently flags
    // the record as overridden.
    @PutMapping("/{damageId}/override")
    @PreAuthorize(DAMAGE_WRITE_ROLES)
    @Auditable(action = "DAMAGE_OVERRIDDEN", entityType = "Damage")
    public ApiResponse<DamageResponse> overrideConsent(
            @PathVariable Long damageId,
            @Valid @RequestBody DamageOverrideRequest request
    ) {
        return ApiResponse.success("Damage approved by override",
                damageService.overrideConsent(damageId, request.reason(), accessGuard.currentUserId()));
    }

    @PutMapping("/{damageId}/cancel")
    @PreAuthorize(DAMAGE_WRITE_ROLES)
    @Auditable(action = "DAMAGE_CANCELLED", entityType = "Damage")
    public ApiResponse<DamageResponse> cancelDamage(@PathVariable Long damageId) {
        return ApiResponse.success("Damage cancelled", damageService.cancelDamage(damageId));
    }

    // Optional ?status=PENDING_CONSENT|APPROVED|CANCELLED|DEDUCTED and ?employeeId=. Newest first.
    @GetMapping
    @PreAuthorize(DAMAGE_READ_ROLES)
    public ApiResponse<List<DamageResponse>> getDamages(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long employeeId
    ) {
        return ApiResponse.success(damageService.getDamages(status, employeeId));
    }

    @GetMapping("/{damageId}")
    @PreAuthorize(DAMAGE_READ_ROLES)
    public ApiResponse<DamageResponse> getDamage(@PathVariable Long damageId) {
        return ApiResponse.success(damageService.getDamageById(damageId));
    }
}
