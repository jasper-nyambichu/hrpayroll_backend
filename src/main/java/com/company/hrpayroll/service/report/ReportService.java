// service/report/ReportService.java
package com.company.hrpayroll.service.report;

import com.company.hrpayroll.dto.response.common.GeneratedDocument;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    // Reuses common/pdf's PdfDocumentWriter pattern rather than
    // reinventing PDF generation a second time — report output is built the
    // same way agent documents are.
    GeneratedDocument generateAttendanceReport(Long branchId, LocalDate start, LocalDate end);
    GeneratedDocument generateEmployeeListReport(List<Long> branchIds);
}