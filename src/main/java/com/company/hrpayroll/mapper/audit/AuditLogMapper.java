// mapper/audit/AuditLogMapper.java
package com.company.hrpayroll.mapper.audit;

import com.company.hrpayroll.dto.response.audit.AuditLogResponse;
import com.company.hrpayroll.entity.audit.AuditLog;
import org.springframework.stereotype.Service;

@Service
public class AuditLogMapper {

    public AuditLogResponse toAuditLogResponse(AuditLog log, String performedByUsername) {
        return new AuditLogResponse(
                log.getId(),
                log.getPerformedByUserId(),
                performedByUsername,
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetailsJson(),
                log.getCreatedAt()
        );
    }
}