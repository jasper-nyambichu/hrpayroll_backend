// security/AccessGuard.java
package com.company.hrpayroll.security;

import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Used inside @PreAuthorize expressions across every module, e.g.
// @PreAuthorize("hasRole('SUPER_ADMIN') or @accessGuard.isSameBranch(#branchId)")
@Component("accessGuard")
@RequiredArgsConstructor
public class AccessGuard {

    private final EmployeeRepository employeeRepository;

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

    // The fix: looks up the EMPLOYEE's actual branch from the database,
    // rather than trusting a branch ID the caller supplied. This is what
    // closes the gap — a Branch Manager passing an arbitrary employeeId can
    // no longer bypass scoping just because no branchId appeared in the URL.
    // Returns false (not an exception) for an unknown employeeId, since a
    // security check should fail closed, not leak a 404 vs 403 distinction.
    public boolean isEmployeeInMyBranch(Long employeeId) {
        User user = currentUser();
        if (user.getBranchId() == null) {
            return false;
        }
        return employeeRepository.findById(employeeId)
                .map(employee -> user.getBranchId().equals(employee.getBranchId()))
                .orElse(false);
    }

    // Same pattern for agents — used once EmployeeDocumentController and
    // similar per-employee endpoints need agent-side scoping too.
    public boolean isEmployeeOfMyAgent(Long employeeId) {
        User user = currentUser();
        if (user.getAgentId() == null) {
            return false;
        }
        return employeeRepository.findById(employeeId)
                .map(employee -> user.getAgentId().equals(employee.getAgentId()))
                .orElse(false);
    }

    public Long currentBranchId() {
        return currentUser().getBranchId();
    }

    public Long currentAgentId() {
        return currentUser().getAgentId();
    }

    public Long currentEmployeeId() {
        return currentUser().getId();
    }

    public Long currentUserId() {
        return currentUser().getId();
    }
}