// service/company/impl/CompanyServiceImpl.java
package com.company.hrpayroll.service.company.impl;

import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.service.common.pdf.PdfDocumentWriter;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.UncheckedIOException;
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

    private static final int MAX_LOGO_BYTES = 1024 * 1024;
    private static final String LOGO_PATH = "/company/logo";

    @Override
    public CompanyResponse getCompany() {
        // Single-company system for now — always the first (and only) record.
        Company company = companyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Company has not been set up yet"));
        return companyMapper.toCompanyResponse(company);
    }

    @Override
    @Transactional
    public CompanyResponse uploadLogo(MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the uploaded logo", e);
        }

        if (content.length == 0) {
            throw new InvalidOperationException("Choose a logo file");
        }
        if (content.length > MAX_LOGO_BYTES) {
            throw new InvalidOperationException("The logo must be 1 MB or smaller");
        }

        // Going by the file's first bytes, not the name or type the browser claims it is.
        String contentType = detectImageType(content);
        if (contentType == null) {
            throw new InvalidOperationException("The logo must be a PNG or JPEG image");
        }

        Company company = findCompanyOrThrow();
        company.setLogoData(content);
        company.setLogoContentType(contentType);
        company.setLogoUrl(LOGO_PATH);
        return companyMapper.toCompanyResponse(companyRepository.save(company));
    }

    @Override
    @Transactional
    public CompanyResponse removeLogo() {
        Company company = findCompanyOrThrow();
        company.setLogoData(null);
        company.setLogoContentType(null);
        company.setLogoUrl(null);
        return companyMapper.toCompanyResponse(companyRepository.save(company));
    }

    @Override
    public GeneratedDocument getLogo() {
        Company company = findCompanyOrThrow();
        if (company.getLogoData() == null) {
            throw new ResourceNotFoundException("No logo has been uploaded");
        }
        return new GeneratedDocument("logo", company.getLogoContentType(), company.getLogoData());
    }

    @Override
    public PdfDocumentWriter.Letterhead getLetterhead() {
        return companyRepository.findAll().stream().findFirst()
                .map(company -> new PdfDocumentWriter.Letterhead(
                        company.getName(), company.getLogoData(), company.getAddress(), company.getPhone(), company.getEmail()))
                .orElse(null);
    }

    private Company findCompanyOrThrow() {
        return companyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Company has not been set up yet"));
    }

    private static String detectImageType(byte[] content) {
        if (content.length >= 4 && (content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G') {
            return "image/png";
        }
        if (content.length >= 3 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8 && (content[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return null;
    }

    // Agent payments cannot be calculated without a settings row, so the first time the company is
    // saved a default settings row (VAT 16%, 8-hour day, Sat/Sun weekend) is created with it. The
    // Admin then adjusts the values in Settings.
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