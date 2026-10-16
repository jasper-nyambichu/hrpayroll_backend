// service/company/impl/CompanyServiceImpl.java
package com.company.hrpayroll.service.company.impl;

import com.company.hrpayroll.dto.request.company.CompanyRequest;
import com.company.hrpayroll.dto.response.company.CompanyResponse;
import com.company.hrpayroll.entity.company.Company;
import com.company.hrpayroll.entity.company.CompanySettings;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.company.CompanyMapper;
import com.company.hrpayroll.repository.company.CompanyRepository;
import com.company.hrpayroll.repository.company.CompanySettingsRepository;
import com.company.hrpayroll.service.company.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final CompanyMapper companyMapper;

    @Override
    public CompanyResponse getCompany() {
        // Single-company system for now — always the first (and only) record.
        Company company = companyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Company has not been set up yet"));
        return companyMapper.toCompanyResponse(company);
    }

    // Payroll cannot run without a settings row, so the first time the company is
    // saved a default settings row (VAT 16%, 8-hour day, Sat/Sun weekend, monthly
    // pay) is created with it. Super Admin then adjusts the values in Settings.
    @Override
    @Transactional
    public CompanyResponse createOrUpdateCompany(CompanyRequest request) {
        Company company = companyRepository.findAll().stream().findFirst()
                .orElseGet(() -> companyMapper.toCompany(request));

        if (company.getId() != null) {
            companyMapper.updateCompanyFromRequest(company, request);
        }

        Company saved = companyRepository.save(company);

        if (companySettingsRepository.findAll().isEmpty()) {
            companySettingsRepository.save(CompanySettings.builder()
                    .companyId(saved.getId())
                    .vatRate(new BigDecimal("16.00"))
                    .build());
        }

        return companyMapper.toCompanyResponse(saved);
    }
}