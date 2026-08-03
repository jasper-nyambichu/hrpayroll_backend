// service/company/CompanySettingsService.java
package com.company.hrpayroll.service.company;

import com.company.hrpayroll.dto.request.company.CompanySettingsUpdateRequest;
import com.company.hrpayroll.dto.response.company.CompanySettingsResponse;

import java.math.BigDecimal;

public interface CompanySettingsService {
    CompanySettingsResponse getSettings();
    CompanySettingsResponse updateSettings(CompanySettingsUpdateRequest request);

    // The one method the payroll module will call directly — kept as a
    // simple BigDecimal return so payroll/agentformula doesn't need to know
    // about the settings DTO at all.
    BigDecimal getCurrentVatRate();
}