// dto/response/company/CompanyResponse.java
package com.company.hrpayroll.dto.response.company;

public record CompanyResponse(
        Long id,
        String name,
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