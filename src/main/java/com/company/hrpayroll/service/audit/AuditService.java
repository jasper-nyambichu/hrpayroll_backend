// service/audit/AuditService.java
package com.company.hrpayroll.service.audit;

import com.company.hrpayroll.dto.response.PageResponse;
import com.company.hrpayroll.dto.response.audit.AuditLogResponse;
import org.springframework.data.domain.Pageable;

public interface AuditService {
    // Called by AuditAspect (config/AOP, not written yet — the aspect calls
    // this to actually persist a log entry after a write method succeeds).
    void record(Long performedByUserId, String action, String entityType, Long entityId, String detailsJson);

    PageResponse<AuditLogResponse> getLogsForEntity(String entityType, Long entityId, Pageable pageable);
    PageResponse<AuditLogResponse> getLogsByUser(Long userId, Pageable pageable);
}