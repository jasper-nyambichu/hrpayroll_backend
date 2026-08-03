// dto/request/company/CompanyRequest.java
package com.company.hrpayroll.dto.request.company;

import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank(message = "Company name is required") String name,
        String logoUrl,
        String address,
        String phone,
        String email,
        String website,
        String kraPin,
        String shifNumber,
        String nssfNumber,
        String nitaNumber,
        String currency,
        String timezone
) {}