// controller/company/PublicOrganisationController.java
package com.company.hrpayroll.controller.company;

import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.company.CompanyResponse;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.service.company.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * The organisation using this system, as the sign-in pages show it before anyone has signed in: its name
 * and the logo uploaded under Admin → Settings. Nothing else about the company is exposed here.
 * permitAll() in SecurityConfig.
 */
@RestController
@RequestMapping("/public/organisation")
@RequiredArgsConstructor
public class PublicOrganisationController {

    private final CompanyService companyService;

    public record OrganisationBranding(String name, boolean hasLogo, String logoVersion) {}

    @GetMapping
    public ApiResponse<OrganisationBranding> get() {
        try {
            CompanyResponse company = companyService.getCompany();
            boolean hasLogo = company.logoUrl() != null;
            // A fingerprint of the image itself, so browsers fetch a newly uploaded logo straight away.
            String version = hasLogo ? Integer.toHexString(Arrays.hashCode(companyService.getLogo().content())) : null;
            return ApiResponse.success(new OrganisationBranding(company.name(), hasLogo, version));
        } catch (ResourceNotFoundException notSetUp) {
            return ApiResponse.success(new OrganisationBranding(null, false, null));
        }
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        GeneratedDocument logo = companyService.getLogo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(logo.content());
    }
}
