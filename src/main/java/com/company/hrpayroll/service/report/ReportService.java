// service/report/ReportService.java
package com.company.hrpayroll.service.report;

import com.company.hrpayroll.dto.response.payroll.GeneratedDocument;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    // Reuses payroll/documents' PdfDocumentWriter pattern rather than
    // reinventing PDF generation a second time — report output is built the
    // same way agent documents are.
    GeneratedDocument generateAttendanceReport(Long branchId, LocalDate start, LocalDate end);
    GeneratedDocument generatePayrollRegisterReport(Long payrollRunId);
    GeneratedDocument generateEmployeeListReport(List<Long> branchIds);
}