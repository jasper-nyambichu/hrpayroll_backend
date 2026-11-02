// controller/agent/AgentController.java
package com.company.hrpayroll.controller.agent;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.agent.AgentRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.agent.AgentResponse;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.agent.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AccessGuard accessGuard;

    // Just enough to pick an agent on a form: id and name, active agents only.
    public record AgentOption(Long id, String name) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "AGENT_CREATED", entityType = "Agent")
    public ApiResponse<AgentResponse> createAgent(@Valid @RequestBody AgentRequest request) {
        return ApiResponse.success("Agent created", agentService.createAgent(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "AGENT_UPDATED", entityType = "Agent")
    public ApiResponse<AgentResponse> updateAgent(
            @PathVariable Long id, @Valid @RequestBody AgentRequest request
    ) {
        return ApiResponse.success("Agent updated", agentService.updateAgent(id, request));
    }

    @PostMapping(value = "/{id}/director-id", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Auditable(action = "AGENT_DIRECTOR_ID_UPLOADED", entityType = "Agent")
    public ApiResponse<AgentResponse> uploadDirectorId(@PathVariable Long id, @RequestParam MultipartFile file) {
        return ApiResponse.success("Director ID saved", agentService.uploadDirectorId(id, file));
    }

    // An identity document: the Admin only, never cached.
    @GetMapping("/{id}/director-id")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> getDirectorId(@PathVariable Long id) {
        GeneratedDocument photo = agentService.getDirectorId(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(photo.content());
    }

    // FINANCE_OFFICER and AUDITOR read agents as part of their financial review (read-only).
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER', 'FINANCE_OFFICER', 'AUDITOR') or @accessGuard.isSameAgent(#id)")
    public ApiResponse<AgentResponse> getAgentById(@PathVariable Long id) {
        return ApiResponse.success(agentService.getAgentById(id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('AGENT')")
    public ApiResponse<AgentResponse> getMyAgentProfile() {
        return ApiResponse.success(agentService.getAgentById(accessGuard.currentAgentId()));
    }

    // HR_MANAGER needs this list to assign an agent on the Add Employee form.
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PAYROLL_OFFICER', 'HR_MANAGER', 'FINANCE_OFFICER', 'AUDITOR')")
    public ApiResponse<List<AgentResponse>> getAllAgents() {
        return ApiResponse.success(agentService.getAllAgents());
    }

    // Id and name of the active agents, for choosing one on a form. Branch Managers use this when
    // adding an employee (the chosen agent is notified); it exposes none of an agent's other details.
    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'BRANCH_MANAGER')")
    public ApiResponse<List<AgentOption>> getAgentOptions() {
        return ApiResponse.success(agentService.getAllAgents().stream()
                .filter(agent -> "ACTIVE".equals(agent.status()))
                .map(agent -> new AgentOption(agent.id(), agent.name()))
                .toList());
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "AGENT_DEACTIVATED", entityType = "Agent")
    public ApiResponse<Void> deactivateAgent(@PathVariable Long id) {
        agentService.deactivateAgent(id);
        return ApiResponse.success("Agent deactivated", null);
    }
}