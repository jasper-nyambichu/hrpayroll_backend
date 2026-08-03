// mapper/company/CompanySettingsMapper.java
package com.company.hrpayroll.mapper.company;

import com.company.hrpayroll.dto.request.company.CompanySettingsUpdateRequest;
import com.company.hrpayroll.dto.response.company.CompanySettingsResponse;
import com.company.hrpayroll.entity.company.CompanySettings;
import org.springframework.stereotype.Service;

@Service
public class CompanySettingsMapper {

    public void updateSettingsFromRequest(CompanySettings settings, CompanySettingsUpdateRequest dto) {
        settings.setVatRate(dto.vatRate());
        if (dto.defaultWorkingHoursPerDay() != null) {
            settings.setDefaultWorkingHoursPerDay(dto.defaultWorkingHoursPerDay());
        }
        if (dto.overtimeMultiplier() != null) {
            settings.setOvertimeMultiplier(dto.overtimeMultiplier());
        }
        if (dto.weekendDays() != null) {
            settings.setWeekendDays(dto.weekendDays());
        }
        if (dto.payrollPeriod() != null) {
            settings.setPayrollPeriod(dto.payrollPeriod());
        }
    }

    public CompanySettingsResponse toCompanySettingsResponse(CompanySettings settings) {
        return new CompanySettingsResponse(
                settings.getId(),
                settings.getVatRate(),
                settings.getDefaultWorkingHoursPerDay(),
                settings.getOvertimeMultiplier(),
                settings.getWeekendDays(),
                settings.getPayrollPeriod()
        );
    }
}