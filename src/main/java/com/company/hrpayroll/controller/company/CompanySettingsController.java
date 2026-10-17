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