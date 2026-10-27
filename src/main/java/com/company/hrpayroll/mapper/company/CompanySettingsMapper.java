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
        if (dto.dailyRate() != null) {
            settings.setDailyRate(dto.dailyRate());
        }
        if (dto.shifMode() != null) {
            settings.setShifMode(dto.shifMode());
        }
        if (dto.shifValue() != null) {
            settings.setShifValue(dto.shifValue());
        }
        if (dto.nssfMode() != null) {
            settings.setNssfMode(dto.nssfMode());
        }
        if (dto.nssfValue() != null) {
            settings.setNssfValue(dto.nssfValue());
        }
        if (dto.applyHousingLevy() != null) {
            settings.setApplyHousingLevy(dto.applyHousingLevy());
        }
        if (dto.applyPaye() != null) {
            settings.setApplyPaye(dto.applyPaye());
        }
        if (dto.applyNita() != null) {
            settings.setApplyNita(dto.applyNita());
        }
        if (dto.deductionCeilingPercent() != null) {
            settings.setDeductionCeilingPercent(dto.deductionCeilingPercent());
        }
    }

    public CompanySettingsResponse toCompanySettingsResponse(CompanySettings settings) {
        return new CompanySettingsResponse(
                settings.getId(),
                settings.getVatRate(),
                settings.getDefaultWorkingHoursPerDay(),
                settings.getOvertimeMultiplier(),
                settings.getWeekendDays(),
                settings.getPayrollPeriod(),
                settings.getDailyRate(),
                settings.getShifMode(),
                settings.getShifValue(),
                settings.getNssfMode(),
                settings.getNssfValue(),
                settings.isApplyHousingLevy(),
                settings.isApplyPaye(),
                settings.isApplyNita(),
                settings.getDeductionCeilingPercent()
        );
    }
}