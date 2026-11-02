// controller/agentadvance/AgentAdvanceController.java
package com.company.hrpayroll.controller.agentadvance;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRecoveryRequest;
import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRejectionRequest;
import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRequest;
import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceSubmitRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.agentadvance.AgentAdvanceResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.agentadvance.AgentAdvanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agent-advances")
@RequiredArgsConstructor
public class AgentAdvanceController {

    private final AgentAdvanceService agentAdvanceService;
    private final AccessGuard accessGuard;

    private static final String STAFF_READ_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER', 'FINANCE_OFFICER', 'AUDITOR')";

    // Recording and manually recovering advances is a Payroll Officer
    // operation (proposal §4) — Finance Officer / Auditor are read-only and
    // agents can view their own advances (below) but never create or
    // recover one themselves.
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PAYROLL_OFFICER')")
    @Auditable(action = "AGENT_ADVANCE_RECORDED", entityType = "AgentAdvance")
    public ApiResponse<AgentAdvanceResponse> recordAdvance(@Valid @RequestBody AgentAdvanceRequest request) {
        return ApiResponse.success("Advance recorded", agentAdvanceService.recordAdvance(request));
    }

    // Manual recovery outside of an agent payment (e.g. the agent pays cash
    // directly) — automatic recovery happens inside
    // AgentSettlementServiceImpl and never touches this endpoint.
    @PutMapping("/{advanceId}/recover")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PAYROLL_OFFICER')")
    @Auditable(action = "AGENT_ADVANCE_RECOVERED", entityType = "AgentAdvance")
    public ApiResponse<AgentAdvanceResponse> recoverAmount(
            @PathVariable Long advanceId, @Valid @RequestBody AgentAdvanceRecoveryRequest request
    ) {
        return ApiResponse.success("Advance recovery applied", agentAdvanceService.recoverAmount(advanceId, request));
    }

    // Company-wide list, newest first.
    @GetMapping
    @PreAuthorize(STAFF_READ_ROLES)
    public ApiResponse<List<AgentAdvanceResponse>> getAllAdvances() {
        return ApiResponse.success(agentAdvanceService.getAllAdvances());
    }

    @GetMapping("/{advanceId}")
    @PreAuthorize(STAFF_READ_ROLES)
    public ApiResponse<AgentAdvanceResponse> getAdvance(@PathVariable Long advanceId) {
        return ApiResponse.success(agentAdvanceService.getAdvanceById(advanceId));
    }

    @GetMapping("/by-agent/{agentId}")
    @PreAuthorize(STAFF_READ_ROLES + " or @accessGuard.isSameAgent(#agentId)")
    public ApiResponse<List<AgentAdvanceResponse>> getAdvancesByAgent(@PathVariable Long agentId) {
        return ApiResponse.success(agentAdvanceService.getAdvancesByAgent(agentId));
    }

    // Agent Portal convenience endpoint — same /me pattern as AgentController.
    @GetMapping("/my-advances")
    @PreAuthorize("hasRole('AGENT')")
    public ApiResponse<List<AgentAdvanceResponse>> getMyAdvances() {
        return ApiResponse.success(agentAdvanceService.getAdvancesByAgent(accessGuard.currentAgentId()));
    }

    // An agent asks the Admin for an advance. It is never recovered from payroll until the Admin approves it.
    @PostMapping("/request")
    @PreAuthorize("hasRole('AGENT')")
    @Auditable(action = "AGENT_ADVANCE_REQUESTED", entityType = "AgentAdvance")
    public ApiResponse<AgentAdvanceResponse> requestAdvance(@Valid @RequestBody AgentAdvanceSubmitRequest request) {
        return ApiResponse.success("Advance request sent to the Administrator",
                agentAdvanceService.requestAdvance(request, accessGuard.currentAgentId()));
    }

    // Deciding an advance request is the Admin's call (Super Admin's powers belong to Admin).
    @PutMapping("/{advanceId}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "AGENT_ADVANCE_APPROVED", entityType = "AgentAdvance")
    public ApiResponse<AgentAdvanceResponse> approveAdvance(@PathVariable Long advanceId) {
        return ApiResponse.success("Advance approved",
                agentAdvanceService.approveAdvance(advanceId, accessGuard.currentUserId()));
    }

    @PutMapping("/{advanceId}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "AGENT_ADVANCE_REJECTED", entityType = "AgentAdvance")
    public ApiResponse<AgentAdvanceResponse> rejectAdvance(
            @PathVariable Long advanceId, @Valid @RequestBody AgentAdvanceRejectionRequest request
    ) {
        return ApiResponse.success("Advance rejected",
                agentAdvanceService.rejectAdvance(advanceId, request.reason(), accessGuard.currentUserId()));
    }
}