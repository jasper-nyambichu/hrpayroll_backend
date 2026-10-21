// security/RoleHierarchyConfig.java
package com.company.hrpayroll.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

/**
 * The company runs with ONE top role: Admin. Admin therefore holds every authority Super Admin
 * has — company profile, settings, user management, branch activation, the history log —
 * without rewriting each @PreAuthorize. Existing Super Admin accounts are unaffected.
 *
 * "ROLE_ADMIN > ROLE_SUPER_ADMIN" means an ADMIN passes hasRole('SUPER_ADMIN') checks too.
 * The handler is declared explicitly (and static, as method-security infrastructure must be)
 * so method security is guaranteed to use the hierarchy.
 */
@Configuration
public class RoleHierarchyConfig {

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_SUPER_ADMIN");
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }
}