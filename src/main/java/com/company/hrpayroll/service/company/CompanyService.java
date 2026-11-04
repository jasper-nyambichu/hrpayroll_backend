// service/company/CompanyService.java
package com.company.hrpayroll.service.company;

import com.company.hrpayroll.dto.request.company.CompanyRequest;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.company.CompanyResponse;
import com.company.hrpayroll.service.common.pdf.PdfDocumentWriter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CompanyService {
    CompanyResponse getCompany();
    CompanyResponse createOrUpdateCompany(CompanyRequest request);

    // PNG or JPEG, at most 1 MB. The logo only ever changes through these two.
    CompanyResponse uploadLogo(MultipartFile file);
    CompanyResponse removeLogo();

    GeneratedDocument getLogo();

    // The current logo and contact details for a generated document's header. Null when not set up.
    PdfDocumentWriter.Letterhead getLetterhead();
}