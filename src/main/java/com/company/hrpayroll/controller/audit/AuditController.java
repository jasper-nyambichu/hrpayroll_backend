// controller/audit/AuditController.java
package com.company.hrpayroll.controller.audit;

import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.PageResponse;
import com.company.hrpayroll.dto.response.audit.AuditLogResponse;
import com.company.hrpayroll.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Read-only by design — matching proposal §4: "Finance Officer / Auditor —
// Read access to ... the full audit trail for compliance review, without
// operational editing rights." There is intentionally no write/delete
// endpoint anywhere in this controller.
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'AUDITOR')")
    public ApiResponse<PageResponse<AuditLogResponse>> getLogsForEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(auditService.getLogsForEntity(entityType, entityId, pageable));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'AUDITOR')")
    public ApiResponse<PageResponse<AuditLogResponse>> getLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(auditService.getLogsByUser(userId, pageable));
    }
}