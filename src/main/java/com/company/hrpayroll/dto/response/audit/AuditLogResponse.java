// dto/response/audit/AuditLogResponse.java
package com.company.hrpayroll.dto.response.audit;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long performedByUserId,
        String performedByUsername,
        String action,
        String entityType,
        Long entityId,
        String detailsJson,
        LocalDateTime createdAt
) {}