// service/audit/AuditService.java
package com.company.hrpayroll.service.audit;

import com.company.hrpayroll.dto.response.PageResponse;
import com.company.hrpayroll.dto.response.audit.AuditFilterOptionsResponse;
import com.company.hrpayroll.dto.response.audit.AuditLogResponse;
import com.company.hrpayroll.dto.response.audit.AuditSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AuditService {
    // Called by AuditAspect (and the login flow) to persist an entry. Entries are append-only.
    void record(Long performedByUserId, String action, String entityType, Long entityId, String detailsJson);

    // The full history log, newest first. Every filter is optional.
    PageResponse<AuditLogResponse> search(
            String action, String entityType, Long userId, String category, LocalDate from, LocalDate to, Pageable pageable);

    AuditLogResponse getById(Long id);

    PageResponse<AuditLogResponse> getLogsForEntity(String entityType, Long entityId, Pageable pageable);
    PageResponse<AuditLogResponse> getLogsByUser(Long userId, Pageable pageable);

    AuditSummaryResponse summary(LocalDate from, LocalDate to);
    AuditFilterOptionsResponse filterOptions();
}