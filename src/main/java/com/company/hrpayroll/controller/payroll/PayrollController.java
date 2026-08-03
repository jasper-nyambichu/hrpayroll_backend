// controller/payroll/PayrollController.java
package com.company.hrpayroll.controller.payroll;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.payroll.PayrollRunRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.payroll.AgentPayrollSummaryResponse;
import com.company.hrpayroll.dto.response.payroll.GeneratedDocument;
import com.company.hrpayroll.dto.response.payroll.PayrollRunResponse;
import com.company.hrpayroll.dto.response.payroll.PayslipResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.payroll.batch.PayrollRunService;
import com.company.hrpayroll.service.payroll.documents.PayrollDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollRunService payrollRunService;
    private final PayrollDocumentService payrollDocumentService;
    private final AccessGuard accessGuard;

    private static final String STAFF_READ_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER', 'FINANCE_OFFICER', 'AUDITOR')";

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER')")
    @Auditable(action = "PAYROLL_RUN_EXECUTED", entityType = "PayrollRun")
    public ApiResponse<PayrollRunResponse> runPayroll(@Valid @RequestBody PayrollRunRequest request) {
        return ApiResponse.success(
                "Payroll run completed",
                payrollRunService.runPayroll(request, accessGuard.currentUserId())
        );
    }

    @GetMapping("/runs/{runId}")
    @PreAuthorize(STAFF_READ_ROLES)
    public ApiResponse<PayrollRunResponse> getRun(@PathVariable Long runId) {
        return ApiResponse.success(payrollRunService.getRunById(runId));
    }

    @GetMapping("/runs/{runId}/payslips")
    @PreAuthorize(STAFF_READ_ROLES)
    public ApiResponse<List<PayslipResponse>> getPayslips(@PathVariable Long runId) {
        return ApiResponse.success(payrollRunService.getPayslipsForRun(runId));
    }

    @GetMapping("/runs/{runId}/agent-summaries")
    @PreAuthorize(STAFF_READ_ROLES)
    public ApiResponse<List<AgentPayrollSummaryResponse>> getAgentSummaries(@PathVariable Long runId) {
        return ApiResponse.success(payrollRunService.getAgentSummariesForRun(runId));
    }

    // The three document downloads from proposal §6.4. Staff with the read
    // roles above can pull any agent's documents; an agent logged into
    // their own portal can only pull their own, via isSameAgent — same
    // access pattern used for every agent-scoped endpoint so far.

    @GetMapping("/runs/{runId}/agents/{agentId}/invoice")
    @PreAuthorize(STAFF_READ_ROLES + " or @accessGuard.isSameAgent(#agentId)")
    public ResponseEntity<byte[]> downloadAgentInvoice(
            @PathVariable Long runId, @PathVariable Long agentId
    ) {
        return toPdfDownload(payrollDocumentService.getAgentInvoice(runId, agentId));
    }

    @GetMapping("/runs/{runId}/agents/{agentId}/voucher")
    @PreAuthorize(STAFF_READ_ROLES + " or @accessGuard.isSameAgent(#agentId)")
    public ResponseEntity<byte[]> downloadPaymentVoucher(
            @PathVariable Long runId, @PathVariable Long agentId
    ) {
        return toPdfDownload(payrollDocumentService.getPaymentVoucher(runId, agentId));
    }

    @GetMapping("/runs/{runId}/agents/{agentId}/summary")
    @PreAuthorize(STAFF_READ_ROLES + " or @accessGuard.isSameAgent(#agentId)")
    public ResponseEntity<byte[]> downloadAgentPayrollSummary(
            @PathVariable Long runId, @PathVariable Long agentId
    ) {
        return toPdfDownload(payrollDocumentService.getAgentPayrollSummaryDocument(runId, agentId));
    }

    // Binary file responses deliberately do NOT go through ApiResponse<T> —
    // wrapping a PDF's bytes in a JSON envelope would corrupt the file for
    // any client trying to download and open it directly.
    private ResponseEntity<byte[]> toPdfDownload(GeneratedDocument document) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", document.fileName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(document.content());
    }
}