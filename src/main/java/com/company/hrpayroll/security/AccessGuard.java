// security/AccessGuard.java
package com.company.hrpayroll.security;

import com.company.hrpayroll.entity.auth.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Used inside @PreAuthorize expressions across every module, e.g.
// @PreAuthorize("hasRole('SUPER_ADMIN') or @accessGuard.isSameBranch(#branchId)")
@Component("accessGuard")
public class AccessGuard {

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public boolean isSameBranch(Long branchId) {
        User user = currentUser();
        return user.getBranchId() != null && user.getBranchId().equals(branchId);
    }

    public boolean isSameAgent(Long agentId) {
        User user = currentUser();
        return user.getAgentId() != null && user.getAgentId().equals(agentId);
    }

    public boolean isSameEmployee(Long employeeId) {
        User user = currentUser();
        return user.getEmployeeId() != null && user.getEmployeeId().equals(employeeId);
    }

    public Long currentBranchId() {
        return currentUser().getBranchId();
    }

    public Long currentAgentId() {
        return currentUser().getAgentId();
    }

    public Long currentEmployeeId() {
        return currentUser().getEmployeeId();
    }

    public Long currentUserId() {
        return currentUser().getId();
    }
}