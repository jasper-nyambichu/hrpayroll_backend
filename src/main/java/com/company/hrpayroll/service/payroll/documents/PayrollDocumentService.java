// service/payroll/documents/PayrollDocumentService.java
package com.company.hrpayroll.service.payroll.documents;

import com.company.hrpayroll.dto.response.payroll.GeneratedDocument;

// Fetches the right entities and delegates to the three generators.
// Exists so PayrollController never injects a repository directly —
// every other controller in this system goes through a service, and this
// keeps that consistent for document downloads too.
public interface PayrollDocumentService {
    GeneratedDocument getAgentInvoice(Long payrollRunId, Long agentId);
    GeneratedDocument getPaymentVoucher(Long payrollRunId, Long agentId);
    GeneratedDocument getAgentPayrollSummaryDocument(Long payrollRunId, Long agentId);
}