// controller/audit/AuditController.java
package com.company.hrpayroll.controller.audit;

import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.PageResponse;
import com.company.hrpayroll.dto.response.audit.AuditFilterOptionsResponse;
import com.company.hrpayroll.dto.response.audit.AuditLogResponse;
import com.company.hrpayroll.dto.response.audit.AuditSummaryResponse;
import com.company.hrpayroll.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

// Read-only by design — matching proposal §4: "Finance Officer / Auditor — Can read financial
// reports and the full history log, but cannot change anything." There is intentionally no
// write/delete endpoint anywhere in this controller, and the table itself is append-only.
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private static final String AUDIT_READ_ROLES = "hasAnyRole('SUPER_ADMIN', 'AUDITOR', 'FINANCE_OFFICER')";
    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final AuditService auditService;

    // The full history log, newest first. Every filter is optional; dates are inclusive.
    @GetMapping
    @PreAuthorize(AUDIT_READ_ROLES)
    public ApiResponse<PageResponse<AuditLogResponse>> search(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return ApiResponse.success(auditService.search(action, entityType, userId, category, from, to, pageable(page, size)));
    }

    // The values that exist in the log — used to populate the filter dropdowns.
    @GetMapping("/filters")
    @PreAuthorize(AUDIT_READ_ROLES)
    public ApiResponse<AuditFilterOptionsResponse> filters() {
        return ApiResponse.success(auditService.filterOptions());
    }

    // Totals, the login picture and where activity happened over a period (default: last 7 days).
    @GetMapping("/summary")
    @PreAuthorize(AUDIT_READ_ROLES)
    public ApiResponse<AuditSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(auditService.summary(from, to));
    }

    @GetMapping("/{id}")
    @PreAuthorize(AUDIT_READ_ROLES)
    public ApiResponse<AuditLogResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(auditService.getById(id));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize(AUDIT_READ_ROLES)
    public ApiResponse<PageResponse<AuditLogResponse>> getLogsForEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(auditService.getLogsForEntity(entityType, entityId, pageable(page, size)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize(AUDIT_READ_ROLES)
    public ApiResponse<PageResponse<AuditLogResponse>> getLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(auditService.getLogsByUser(userId, pageable(page, size)));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), NEWEST_FIRST);
    }
}