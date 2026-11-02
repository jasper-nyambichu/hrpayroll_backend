// controller/company/CompanyController.java
package com.company.hrpayroll.controller.company;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.company.CompanyRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.company.CompanyResponse;
import com.company.hrpayroll.service.company.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // Any authenticated user can view company details — logos, name, etc.
    // appear across every portal's header.
    @GetMapping
    public ApiResponse<CompanyResponse> getCompany() {
        return ApiResponse.success(companyService.getCompany());
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "COMPANY_UPDATED", entityType = "Company")
    public ApiResponse<CompanyResponse> createOrUpdateCompany(@Valid @RequestBody CompanyRequest request) {
        return ApiResponse.success("Company details saved", companyService.createOrUpdateCompany(request));
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> getLogo() {
        GeneratedDocument logo = companyService.getLogo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .cacheControl(CacheControl.noCache())
                .body(logo.content());
    }

    @PostMapping(value = "/logo", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "COMPANY_LOGO_UPLOADED", entityType = "Company")
    public ApiResponse<CompanyResponse> uploadLogo(@RequestParam MultipartFile file) {
        return ApiResponse.success("Logo saved", companyService.uploadLogo(file));
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "COMPANY_LOGO_REMOVED", entityType = "Company")
    public ApiResponse<CompanyResponse> removeLogo() {
        return ApiResponse.success("Logo removed", companyService.removeLogo());
    }
}