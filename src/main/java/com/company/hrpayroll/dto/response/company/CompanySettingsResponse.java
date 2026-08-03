// dto/response/company/CompanySettingsResponse.java
package com.company.hrpayroll.dto.response.company;

import java.math.BigDecimal;

public record CompanySettingsResponse(
        Long id,
        BigDecimal vatRate,
        Double defaultWorkingHoursPerDay,
        Double overtimeMultiplier,
        String weekendDays,
        String payrollPeriod
) {}