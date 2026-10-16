// dto/request/company/CompanySettingsUpdateRequest.java
package com.company.hrpayroll.dto.request.company;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CompanySettingsUpdateRequest(
        @NotNull(message = "VAT rate is required")
        @DecimalMin(value = "0.0", message = "VAT rate cannot be negative")
        @DecimalMax(value = "100.0", message = "VAT rate cannot exceed 100") BigDecimal vatRate,
        Double defaultWorkingHoursPerDay,
        Double overtimeMultiplier,
        String weekendDays,
        String payrollPeriod
) {}