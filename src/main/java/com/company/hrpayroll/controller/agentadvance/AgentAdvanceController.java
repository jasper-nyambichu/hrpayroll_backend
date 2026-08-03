// controller/agentadvance/AgentAdvanceController.java
package com.company.hrpayroll.controller.agentadvance;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRecoveryRequest;
import com.company.hrpayroll.dto.request.agentadvance.AgentAdvanceRequest;
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

    // Recording and manually recovering advances is a finance operation —
    // agents can view their own advances (below) but never create or
    // recover one themselves.
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PAYROLL_OFFICER', 'FINANCE_OFFICER')")
    @Auditable(action = "AGENT_ADVANCE_RECORDED", entityType = "AgentAdvance")
    public ApiResponse<AgentAdvanceResponse> recordAdvance(@Valid @RequestBody AgentAdvanceRequest request) {
        return ApiResponse.success("Advance recorded", agentAdvanceService.recordAdvance(request));
    }

    // Manual recovery outside of a payroll run (e.g. the agent pays cash
    // directly) — automatic recovery during payroll runs happens inside
    // PayrollRunServiceImpl and never touches this endpoint.
    @PutMapping("/{advanceId}/recover")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PAYROLL_OFFICER', 'FINANCE_OFFICER')")
    @Auditable(action = "AGENT_ADVANCE_RECOVERED", entityType = "AgentAdvance")
    public ApiResponse<AgentAdvanceResponse> recoverAmount(
            @PathVariable Long advanceId, @Valid @RequestBody AgentAdvanceRecoveryRequest request
    ) {
        return ApiResponse.success("Advance recovery applied", agentAdvanceService.recoverAmount(advanceId, request));
    }

    @GetMapping("/by-agent/{agentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER', 'FINANCE_OFFICER') or @accessGuard.isSameAgent(#agentId)")
    public ApiResponse<List<AgentAdvanceResponse>> getAdvancesByAgent(@PathVariable Long agentId) {
        return ApiResponse.success(agentAdvanceService.getAdvancesByAgent(agentId));
    }

    // Agent Portal convenience endpoint — same /me pattern as AgentController.
    @GetMapping("/my-advances")
    @PreAuthorize("hasRole('AGENT')")
    public ApiResponse<List<AgentAdvanceResponse>> getMyAdvances() {
        return ApiResponse.success(agentAdvanceService.getAdvancesByAgent(accessGuard.currentAgentId()));
    }
}