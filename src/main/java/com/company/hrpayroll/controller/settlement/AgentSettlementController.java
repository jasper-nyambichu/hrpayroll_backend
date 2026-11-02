// controller/settlement/AgentSettlementController.java
package com.company.hrpayroll.controller.settlement;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.settlement.AgentSettlementRunRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.dto.response.settlement.AgentSettlementResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.settlement.AgentSettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agent-settlements")
@RequiredArgsConstructor
public class AgentSettlementController {

    private final AgentSettlementService settlementService;
    private final AccessGuard accessGuard;

    private static final String ADMIN_ROLES = "hasAnyRole('SUPER_ADMIN', 'ADMIN')";

    // A Branch Manager the Admin has allowed runs the same calculation for every agent, standing in for
    // the Admin, but is not given the amounts: those stay with the Admin and each agent.
    @PostMapping("/calculate")
    @PreAuthorize(ADMIN_ROLES + " or @accessGuard.canRunAgentPayments()")
    @Auditable(action = "AGENT_PAYMENTS_CALCULATED", entityType = "AgentSettlement")
    public ApiResponse<List<AgentSettlementResponse>> calculate(@Valid @RequestBody AgentSettlementRunRequest request) {
        List<AgentSettlementResponse> results = settlementService.calculate(request, accessGuard.currentUserId());
        return ApiResponse.success("Agent payments calculated", accessGuard.isBranchManager() ? List.of() : results);
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
