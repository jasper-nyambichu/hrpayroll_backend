// service/company/impl/CompanySettingsServiceImpl.java
package com.company.hrpayroll.service.company.impl;

import com.company.hrpayroll.dto.request.company.CompanySettingsUpdateRequest;
import com.company.hrpayroll.dto.response.company.CompanySettingsResponse;
import com.company.hrpayroll.entity.company.Company;
import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.company.CompanySettingsMapper;
import com.company.hrpayroll.repository.company.CompanyRepository;
import com.company.hrpayroll.repository.company.CompanySettingsRepository;
import com.company.hrpayroll.service.company.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanySettingsServiceImpl implements CompanySettingsService {

    private final CompanySettingsRepository companySettingsRepository;
    private final CompanyRepository companyRepository;
    private final CompanySettingsMapper companySettingsMapper;

    @Override
    public CompanySettingsResponse getSettings() {
        CompanySettings settings = currentSettings();
        return companySettingsMapper.toCompanySettingsResponse(settings);
    }

    // Saving creates the settings row on first use (the company must exist first),
    // so a fresh database can be configured entirely through the application.
    @Override
    public CompanySettingsResponse updateSettings(CompanySettingsUpdateRequest request) {
        CompanySettingsUpdateRequest validated = validateAndNormalize(request);

        CompanySettings settings = companySettingsRepository.findAll().stream().findFirst()
                .orElseGet(this::newSettingsForCompany);

        companySettingsMapper.updateSettingsFromRequest(settings, validated);
        CompanySettings saved = companySettingsRepository.save(settings);
        return companySettingsMapper.toCompanySettingsResponse(saved);
    }

    @Override
    public BigDecimal getCurrentVatRate() {
        return currentSettings().getVatRate();
    }

    // The payroll engine parses weekendDays with DayOfWeek.valueOf and prorates on a
    // monthly period, so bad values here would break every payroll run — reject them now.
    private CompanySettingsUpdateRequest validateAndNormalize(CompanySettingsUpdateRequest request) {
        Double hours = request.defaultWorkingHoursPerDay();
        if (hours != null && (hours <= 0 || hours > 24)) {
            throw new InvalidOperationException("Working hours per day must be between 1 and 24");
        }

        Double overtime = request.overtimeMultiplier();
        if (overtime != null && (overtime < 1 || overtime > 5)) {
            throw new InvalidOperationException("Overtime multiplier must be between 1 and 5");
        }

        String period = request.payrollPeriod();
        if (period != null && !"MONTHLY".equalsIgnoreCase(period.trim())) {
            throw new InvalidOperationException("Only a MONTHLY payroll period is supported");
        }

        String weekendDays = request.weekendDays() != null ? normalizeWeekendDays(request.weekendDays()) : null;

        return new CompanySettingsUpdateRequest(
                request.vatRate(),
                hours,
                overtime,
                weekendDays,
                period != null ? "MONTHLY" : null
        );
    }

    // The column holds 20 characters, which fits at most two weekend days.
    private String normalizeWeekendDays(String raw) {
        List<String> days = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(day -> !day.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .toList();

        if (days.isEmpty()) {
            throw new InvalidOperationException("Choose at least one weekend day");
        }
        if (days.size() > 2) {
            throw new InvalidOperationException("At most two weekend days are supported");
        }

        for (String day : days) {
            try {
                DayOfWeek.valueOf(day);
            } catch (IllegalArgumentException e) {
                throw new InvalidOperationException(
                        "'" + day + "' is not a day of the week (use MONDAY to SUNDAY)");
            }
        }

        return days.stream().collect(Collectors.joining(","));
    }

    private CompanySettings newSettingsForCompany() {
        Company company = companyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new InvalidOperationException(
                        "Set up the company details before configuring settings"));

        return CompanySettings.builder()
                .companyId(company.getId())
                .vatRate(BigDecimal.ZERO)
                .build();
    }

    private CompanySettings currentSettings() {
        return companySettingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Company settings have not been configured yet"));
    }
}