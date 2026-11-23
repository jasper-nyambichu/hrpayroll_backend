// controller/company/CompanySettingsController.java
package com.company.hrpayroll.controller.company;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.company.CompanySettingsUpdateRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.company.CompanySettingsResponse;
import com.company.hrpayroll.service.company.CompanySettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// This is where the VAT rate lives — proposal §6.2: "VAT rate — System
// settings (Super Admin only) — can be changed at any time."
@RestController
@RequestMapping("/company/settings")
@RequiredArgsConstructor
public class CompanySettingsController {

    private final CompanySettingsService companySettingsService;
    private final com.company.hrpayroll.service.company.OrganisationOperations operations;

    // How the organisation operates: its work week (hours per day) and the off-day department rule.
    @GetMapping("/operations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER', 'FINANCE_OFFICER', 'AUDITOR', 'BRANCH_MANAGER', 'BACK_OFFICE')")
    public ApiResponse<com.company.hrpayroll.service.company.OrganisationOperations.Operations> getOperations() {
        return ApiResponse.success(operations.get());
    }

    @PutMapping("/operations")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "WORK_WEEK_UPDATED", entityType = "CompanySettings")
    public ApiResponse<com.company.hrpayroll.service.company.OrganisationOperations.Operations> updateOperations(
            @RequestBody com.company.hrpayroll.service.company.OrganisationOperations.Operations request
    ) {
        return ApiResponse.success("Work week saved", operations.update(request));
    }

    // FINANCE_OFFICER and AUDITOR read the rules (e.g. to check the VAT rate on agent invoices).
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER', 'FINANCE_OFFICER', 'AUDITOR')")
    public ApiResponse<CompanySettingsResponse> getSettings() {
        return ApiResponse.success(companySettingsService.getSettings());
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "COMPANY_SETTINGS_UPDATED", entityType = "CompanySettings")
    public ApiResponse<CompanySettingsResponse> updateSettings(
            @Valid @RequestBody CompanySettingsUpdateRequest request
    ) {
        return ApiResponse.success("Settings updated", companySettingsService.updateSettings(request));
    }
}