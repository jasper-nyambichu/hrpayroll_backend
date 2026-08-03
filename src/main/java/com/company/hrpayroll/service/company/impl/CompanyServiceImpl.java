// service/company/impl/CompanyServiceImpl.java
package com.company.hrpayroll.service.company.impl;

import com.company.hrpayroll.dto.request.company.CompanyRequest;
import com.company.hrpayroll.dto.response.company.CompanyResponse;
import com.company.hrpayroll.entity.company.Company;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.company.CompanyMapper;
import com.company.hrpayroll.repository.company.CompanyRepository;
import com.company.hrpayroll.service.company.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Override
    public CompanyResponse getCompany() {
        // Single-company system for now — always the first (and only) record.
        Company company = companyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Company has not been set up yet"));
        return companyMapper.toCompanyResponse(company);
    }

    @Override
    public CompanyResponse createOrUpdateCompany(CompanyRequest request) {
        Company company = companyRepository.findAll().stream().findFirst()
                .orElseGet(() -> companyMapper.toCompany(request));

        if (company.getId() != null) {
            companyMapper.updateCompanyFromRequest(company, request);
        }

        Company saved = companyRepository.save(company);
        return companyMapper.toCompanyResponse(saved);
    }
}