// service/company/impl/CompanySettingsServiceImpl.java
package com.company.hrpayroll.service.company.impl;

import com.company.hrpayroll.dto.request.company.CompanySettingsUpdateRequest;
import com.company.hrpayroll.dto.response.company.CompanySettingsResponse;
import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.company.CompanySettingsMapper;
import com.company.hrpayroll.repository.company.CompanySettingsRepository;
import com.company.hrpayroll.service.company.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CompanySettingsServiceImpl implements CompanySettingsService {

    private final CompanySettingsRepository companySettingsRepository;
    private final CompanySettingsMapper companySettingsMapper;

    @Override
    public CompanySettingsResponse getSettings() {
        CompanySettings settings = currentSettings();
        return companySettingsMapper.toCompanySettingsResponse(settings);
    }

    @Override
    public CompanySettingsResponse updateSettings(CompanySettingsUpdateRequest request) {
        CompanySettings settings = currentSettings();
        companySettingsMapper.updateSettingsFromRequest(settings, request);
        CompanySettings saved = companySettingsRepository.save(settings);
        return companySettingsMapper.toCompanySettingsResponse(saved);
    }

    @Override
    public BigDecimal getCurrentVatRate() {
        return currentSettings().getVatRate();
    }

    private CompanySettings currentSettings() {
        return companySettingsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Company settings have not been configured yet"));
    }
}