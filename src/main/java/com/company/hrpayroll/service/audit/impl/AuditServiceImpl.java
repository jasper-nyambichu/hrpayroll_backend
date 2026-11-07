// service/audit/impl/AuditServiceImpl.java
package com.company.hrpayroll.service.audit.impl;

import com.company.hrpayroll.dto.response.PageResponse;
import com.company.hrpayroll.dto.response.audit.AuditFilterOptionsResponse;
import com.company.hrpayroll.dto.response.audit.AuditLogResponse;
import com.company.hrpayroll.dto.response.audit.AuditSummaryResponse;
import com.company.hrpayroll.entity.audit.AuditLog;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.audit.AuditLogMapper;
import com.company.hrpayroll.repository.audit.AuditLogRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.service.audit.AuditService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final int MAX_SUMMARY_DAYS = 92;
    private static final int DEFAULT_SUMMARY_DAYS = 7;

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
    public PageResponse<AuditLogResponse> search(
            String action, String entityType, Long userId, String category, LocalDate from, LocalDate to, Pageable pageable) {

        Specification<AuditLog> filters = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim()));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("performedByUserId"), userId));
            }
            // The three groups an auditor tracks most. An unknown category matches everything.
            if ("DAMAGES".equals(category)) {
                predicates.add(cb.equal(root.get("entityType"), "Damage"));
            } else if ("LOGINS".equals(category)) {
                predicates.add(cb.like(root.get("action"), "LOGIN%"));
            } else if ("APPROVALS".equals(category)) {
                predicates.add(cb.or(cb.like(root.get("action"), "%APPROVED%"), cb.like(root.get("action"), "%REJECTED%")));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.<LocalDateTime>get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return toPageResponse(auditLogRepository.findAll(filters, pageable));
    }

    @Override
    public AuditLogResponse getById(Long id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log entry not found with id: " + id));
        Map<Long, String> names = usernames(log.getPerformedByUserId() == null ? Set.of() : Set.of(log.getPerformedByUserId()));
        return auditLogMapper.toAuditLogResponse(log, nameOf(names, log.getPerformedByUserId()));
    }

    @Override
    // details_json is a LOB: PostgreSQL only streams it inside a transaction, and derived queries have none.
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getLogsForEntity(String entityType, Long entityId, Pageable pageable) {
        return toPageResponse(auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable));
    }

    @Override
    // details_json is a LOB: PostgreSQL only streams it inside a transaction, and derived queries have none.
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getLogsByUser(Long userId, Pageable pageable) {
        return toPageResponse(auditLogRepository.findByPerformedByUserId(userId, pageable));
    }

    @Override
    public AuditSummaryResponse summary(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_SUMMARY_DAYS - 1L);

        if (end.isBefore(start)) {
            throw new InvalidOperationException("The end date cannot be before the start date");
        }
        if (start.plusDays(MAX_SUMMARY_DAYS).isBefore(end)) {
            throw new InvalidOperationException("A summary can cover at most " + MAX_SUMMARY_DAYS + " days");
        }

        LocalDateTime fromTs = start.atStartOfDay();
        LocalDateTime toTs = end.plusDays(1).atStartOfDay();

        long total = auditLogRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(fromTs, toTs);
        long loginSuccess = countAction("LOGIN_SUCCESS", fromTs, toTs);
        long loginFailed = countAction("LOGIN_FAILED", fromTs, toTs);
        long payrollRuns = countAction("PAYROLL_RUN_EXECUTED", fromTs, toTs);

        List<AuditSummaryResponse.Count> byEntityType = auditLogRepository.countByEntityType(fromTs, toTs).stream()
                .map(row -> new AuditSummaryResponse.Count((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        List<AuditSummaryResponse.Count> byAction = auditLogRepository.countByAction(fromTs, toTs).stream()
                .map(row -> new AuditSummaryResponse.Count((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        List<Object[]> userRows = auditLogRepository.countByUser(fromTs, toTs, PageRequest.of(0, 5));
        Map<Long, String> names = usernames(userRows.stream().map(row -> (Long) row[0]).collect(Collectors.toSet()));
        List<AuditSummaryResponse.UserCount> topUsers = userRows.stream()
                .map(row -> new AuditSummaryResponse.UserCount(
                        (Long) row[0], nameOf(names, (Long) row[0]), ((Number) row[1]).longValue()))
                .toList();

        // One bucket per calendar day in the range, zero-filled so a quiet day still shows.
        Map<LocalDate, Long> perDay = new TreeMap<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            perDay.put(day, 0L);
        }
        for (LocalDateTime createdAt : auditLogRepository.createdAtBetween(fromTs, toTs)) {
            perDay.merge(createdAt.toLocalDate(), 1L, Long::sum);
        }
        List<AuditSummaryResponse.DayCount> daily = perDay.entrySet().stream()
                .map(entry -> new AuditSummaryResponse.DayCount(entry.getKey(), entry.getValue()))
                .toList();

        return new AuditSummaryResponse(
                start, end, total, loginSuccess, loginFailed, payrollRuns, byEntityType, byAction, topUsers, daily);
    }

    @Override
    public AuditFilterOptionsResponse filterOptions() {
        Map<Long, String> names = usernames(new java.util.HashSet<>(auditLogRepository.distinctUserIds()));
        List<AuditFilterOptionsResponse.UserOption> users = names.entrySet().stream()
                .map(entry -> new AuditFilterOptionsResponse.UserOption(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> a.username().compareToIgnoreCase(b.username()))
                .toList();

        return new AuditFilterOptionsResponse(
                auditLogRepository.distinctActions(),
                auditLogRepository.distinctEntityTypes(),
                users
        );
    }

    private long countAction(String action, LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.countByActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(action, from, to);
    }

    // One batched user lookup per page instead of one query per row.
    private PageResponse<AuditLogResponse> toPageResponse(Page<AuditLog> page) {
        Set<Long> userIds = page.getContent().stream()
                .map(AuditLog::getPerformedByUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = usernames(userIds);

        return PageResponse.from(page.map(log ->
                auditLogMapper.toAuditLogResponse(log, nameOf(names, log.getPerformedByUserId()))));
    }

    private Map<Long, String> usernames(Collection<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        if (ids.isEmpty()) {
            return names;
        }
        for (User user : userRepository.findAllById(ids)) {
            names.put(user.getId(), user.getUsername());
        }
        return names;
    }

    private String nameOf(Map<Long, String> names, Long userId) {
        return userId == null ? null : names.get(userId);
    }
}