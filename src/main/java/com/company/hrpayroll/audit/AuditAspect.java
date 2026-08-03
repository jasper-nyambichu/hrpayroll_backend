// audit/AuditAspect.java
package com.company.hrpayroll.audit;

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
        Long entityId = extractEntityId(result);

        auditService.record(userId, auditable.action(), auditable.entityType(), entityId, null);

        return result;
    }

    private Long currentUserIdOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return null; // e.g. biometric sync, authenticated by device token, not a user
        }
        return user.getId();
    }

    // Works for any entity/response type exposing getId() — covers every
    // entity in this system since all of them extend BaseEntity.
    private Long extractEntityId(Object result) {
        try {
            var method = result.getClass().getMethod("getId");
            return (Long) method.invoke(result);
        } catch (Exception e) {
            return null;
        }
    }
}