// audit/AuditAspect.java
package com.company.hrpayroll.audit;

import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// This is the single class making the audit trail automatic across every
// module instead of every service manually calling AuditService. Any method
// annotated @Auditable gets logged after it succeeds, with zero code inside
// the method itself needing to know audit logging exists.
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(auditable)")
    public Object logAuditableAction(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = joinPoint.proceed(); // let the real method run first — only log on success

        Long userId = currentUserIdOrNull();
        Long entityId = extractEntityId(result, joinPoint.getArgs());

        // Descriptive request facts only (method, path, caller address) — never the body.
        auditService.record(userId, auditable.action(), auditable.entityType(), entityId, AuditDetails.withRequest());

        return result;
    }

    private Long currentUserIdOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return null; // e.g. biometric sync, authenticated by device token, not a user
        }
        return user.getId();
    }

    // The id of the record that was acted on. Controllers return ApiResponse<DTO> and the DTOs
    // are records exposing id(), so unwrap the envelope and try both accessor styles. Methods
    // that return nothing (deactivate, delete, approve…) fall back to the first Long argument,
    // which is the path id on every such endpoint.
    private Long extractEntityId(Object result, Object[] args) {
        Object target = result instanceof ApiResponse<?> response ? response.data() : result;

        if (target != null) {
            for (String accessor : new String[]{"id", "getId"}) {
                try {
                    Object value = target.getClass().getMethod(accessor).invoke(target);
                    if (value instanceof Number number) {
                        return number.longValue();
                    }
                } catch (ReflectiveOperationException ignored) {
                    // try the next accessor name
                }
            }
        }

        for (Object arg : args) {
            if (arg instanceof Long id) {
                return id;
            }
        }
        return null;
    }
}