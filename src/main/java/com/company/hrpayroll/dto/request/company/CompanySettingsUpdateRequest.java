// dto/request/company/CompanySettingsUpdateRequest.java
package com.company.hrpayroll.dto.request.company;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Every field after payrollPeriod is optional: leaving it out keeps the saved value, so a screen
// that only edits some settings does not have to resend the rest.
public record CompanySettingsUpdateRequest(
        @NotNull(message = "VAT rate is required")
        @DecimalMin(value = "0.0", message = "VAT rate cannot be negative")
        @DecimalMax(value = "100.0", message = "VAT rate cannot exceed 100") BigDecimal vatRate,
        Double defaultWorkingHoursPerDay,
        Double overtimeMultiplier,
        String weekendDays,
        String payrollPeriod,
        @DecimalMin(value = "0.01", message = "Daily rate must be greater than zero") BigDecimal dailyRate,
        String shifMode,
        @DecimalMin(value = "0.0", message = "SHIF cannot be negative") BigDecimal shifValue,
        String nssfMode,
        @DecimalMin(value = "0.0", message = "NSSF cannot be negative") BigDecimal nssfValue,
        Boolean applyHousingLevy,
        Boolean applyPaye,
        Boolean applyNita,
        @DecimalMin(value = "1.0", message = "Deduction ceiling must be at least 1%")
        @DecimalMax(value = "100.0", message = "Deduction ceiling cannot exceed 100%") BigDecimal deductionCeilingPercent
) {}