// controller/settlement/AgentSettlementController.java
package com.company.hrpayroll.controller.settlement;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.settlement.AgentSettlementRunRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.settlement.AgentPaymentReadinessResponse;
import com.company.hrpayroll.dto.response.settlement.AgentSettlementResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.settlement.AgentSettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/agent-settlements")
@RequiredArgsConstructor
public class AgentSettlementController {

    private final AgentSettlementService settlementService;
    private final AccessGuard accessGuard;

    private static final String ADMIN_ROLES = "hasAnyRole('SUPER_ADMIN', 'ADMIN')";

    @PostMapping("/calculate")
    @PreAuthorize(ADMIN_ROLES)
    @Auditable(action = "AGENT_PAYMENTS_CALCULATED", entityType = "AgentSettlement")
    public ApiResponse<List<AgentSettlementResponse>> calculate(@Valid @RequestBody AgentSettlementRunRequest request) {
        return ApiResponse.success("Agent payments calculated", settlementService.calculate(request, accessGuard.currentUserId()));
    }

    // A dry run of the calculation: every agent's amount and every employee's line, nothing saved or deducted, and
    // nothing an agent can see. The Admin runs the period only once satisfied with it.
    @PostMapping("/preview")
    @PreAuthorize(ADMIN_ROLES)
    public ApiResponse<List<com.company.hrpayroll.dto.response.settlement.AgentPaymentDetail>> preview(
            @Valid @RequestBody AgentSettlementRunRequest request) {
        return ApiResponse.success(settlementService.preview(request));
    }

    // One payment line by line (employees, deductions, each advance with its dates). Admin, or the agent paid.
    @GetMapping("/{settlementId}/detail")
    @PreAuthorize(ADMIN_ROLES + " or @accessGuard.isSettlementOfMyAgent(#settlementId)")
    public ApiResponse<com.company.hrpayroll.dto.response.settlement.AgentPaymentDetail> getDetail(@PathVariable Long settlementId) {
        return ApiResponse.success(settlementService.getDetail(settlementId));
    }

    // What still stands between this period and a calculation. Changes nothing.
    @GetMapping("/readiness")
    @PreAuthorize(ADMIN_ROLES)
    public ApiResponse<AgentPaymentReadinessResponse> getReadiness(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd
    ) {
        return ApiResponse.success(settlementService.getReadiness(periodStart, periodEnd));
    }

    // Every agent's payments, newest period first.
    @GetMapping
    @PreAuthorize(ADMIN_ROLES)
    public ApiResponse<List<AgentSettlementResponse>> getAll() {
        return ApiResponse.success(settlementService.getAll());
    }

    // Agent Portal convenience endpoint — same /me pattern as AgentController.
    @GetMapping("/mine")
    @PreAuthorize("hasRole('AGENT')")
    public ApiResponse<List<AgentSettlementResponse>> getMine() {
        return ApiResponse.success(settlementService.getForAgent(accessGuard.currentAgentId()));
    }

    @GetMapping("/{settlementId}")
    @PreAuthorize(ADMIN_ROLES + " or @accessGuard.isSettlementOfMyAgent(#settlementId)")
    public ApiResponse<AgentSettlementResponse> getOne(@PathVariable Long settlementId) {
        return ApiResponse.success(settlementService.getById(settlementId));
    }

    @GetMapping("/{settlementId}/invoice")
    @PreAuthorize(ADMIN_ROLES + " or @accessGuard.isSettlementOfMyAgent(#settlementId)")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long settlementId) {
        return toPdfDownload(settlementService.getInvoice(settlementId));
    }

    @GetMapping("/{settlementId}/statement")
    @PreAuthorize(ADMIN_ROLES + " or @accessGuard.isSettlementOfMyAgent(#settlementId)")
    public ResponseEntity<byte[]> downloadStatement(@PathVariable Long settlementId) {
        return toPdfDownload(settlementService.getStatement(settlementId));
    }

    // Binary file responses deliberately do NOT go through ApiResponse<T> —
    // wrapping a PDF's bytes in a JSON envelope would corrupt the file.
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
