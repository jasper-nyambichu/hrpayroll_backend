// service/audit/impl/AuditServiceImpl.java
package com.company.hrpayroll.service.audit.impl;

import com.company.hrpayroll.dto.response.PageResponse;
import com.company.hrpayroll.dto.response.audit.AuditLogResponse;
import com.company.hrpayroll.entity.audit.AuditLog;
import com.company.hrpayroll.mapper.audit.AuditLogMapper;
import com.company.hrpayroll.repository.audit.AuditLogRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuditLogMapper auditLogMapper;

    // REQUIRES_NEW is deliberate: an audit entry should be saved even if the
    // business operation it's logging later fails and rolls back elsewhere —
    // and conversely, a failure writing the audit log should never roll back
    // the business operation itself. The two are intentionally independent.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long performedByUserId, String action, String entityType, Long entityId, String detailsJson) {
        AuditLog log = AuditLog.builder()
                .performedByUserId(performedByUserId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .detailsJson(detailsJson)
                .build();
        auditLogRepository.save(log);
    }

    @Override
    public PageResponse<AuditLogResponse> getLogsForEntity(String entityType, Long entityId, Pageable pageable) {
        Page<AuditLogResponse> page = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(this::toResponseWithUsername);
        return PageResponse.from(page);
    }

    @Override
    public PageResponse<AuditLogResponse> getLogsByUser(Long userId, Pageable pageable) {
        Page<AuditLogResponse> page = auditLogRepository.findByPerformedByUserId(userId, pageable)
                .map(this::toResponseWithUsername);
        return PageResponse.from(page);
    }

    private AuditLogResponse toResponseWithUsername(AuditLog log) {
        String username = log.getPerformedByUserId() != null
                ? userRepository.findById(log.getPerformedByUserId())
                .map(u -> u.getUsername()).orElse(null)
                : null;
        return auditLogMapper.toAuditLogResponse(log, username);
    }
}