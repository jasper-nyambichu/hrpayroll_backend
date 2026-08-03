// mapper/company/CompanyMapper.java
package com.company.hrpayroll.mapper.company;

import com.company.hrpayroll.dto.request.company.CompanyRequest;
import com.company.hrpayroll.dto.response.company.CompanyResponse;
import com.company.hrpayroll.entity.company.Company;
import org.springframework.stereotype.Service;

@Service
public class CompanyMapper {

    public Company toCompany(CompanyRequest dto) {
        return Company.builder()
                .name(dto.name())
                .logoUrl(dto.logoUrl())
                .address(dto.address())
                .phone(dto.phone())
                .email(dto.email())
                .website(dto.website())
                .kraPin(dto.kraPin())
                .shifNumber(dto.shifNumber())
                .nssfNumber(dto.nssfNumber())
                .nitaNumber(dto.nitaNumber())
                .currency(dto.currency() != null ? dto.currency() : "KES")
                .timezone(dto.timezone() != null ? dto.timezone() : "Africa/Nairobi")
                .build();
    }

    public void updateCompanyFromRequest(Company company, CompanyRequest dto) {
        company.setName(dto.name());
        company.setLogoUrl(dto.logoUrl());
        company.setAddress(dto.address());
        company.setPhone(dto.phone());
        company.setEmail(dto.email());
        company.setWebsite(dto.website());
        company.setKraPin(dto.kraPin());
        company.setShifNumber(dto.shifNumber());
        company.setNssfNumber(dto.nssfNumber());
        company.setNitaNumber(dto.nitaNumber());
        company.setCurrency(dto.currency());
        company.setTimezone(dto.timezone());
    }

    public CompanyResponse toCompanyResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getLogoUrl(),
                company.getAddress(),
                company.getPhone(),
                company.getEmail(),
                company.getWebsite(),
                company.getKraPin(),
                company.getShifNumber(),
                company.getNssfNumber(),
                company.getNitaNumber(),
                company.getCurrency(),
                company.getTimezone()
        );
    }
}