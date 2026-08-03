// service/company/CompanyService.java
package com.company.hrpayroll.service.company;

import com.company.hrpayroll.dto.request.company.CompanyRequest;
import com.company.hrpayroll.dto.response.company.CompanyResponse;

public interface CompanyService {
    CompanyResponse getCompany();
    CompanyResponse createOrUpdateCompany(CompanyRequest request);
}