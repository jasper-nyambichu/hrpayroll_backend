// security/AccessGuard.java
package com.company.hrpayroll.security;

import com.company.hrpayroll.dto.request.shift.ShiftAssignmentRequest;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.leave.LeaveRequestRepository;
import com.company.hrpayroll.repository.payroll.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Used inside @PreAuthorize expressions across every module, e.g.
// @PreAuthorize("hasRole('SUPER_ADMIN') or @accessGuard.isSameBranch(#branchId)")
@Component("accessGuard")
@RequiredArgsConstructor
public class AccessGuard {

    private final EmployeeRepository employeeRepository;
    private final PayslipRepository payslipRepository;
    private final LeaveRequestRepository leaveRequestRepository;

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

    // Looks up the EMPLOYEE's actual branch from the database rather than trusting a caller-supplied id.
    // Fails closed (false) for an unknown employeeId.
    public boolean isEmployeeInMyBranch(Long employeeId) {
        User user = currentUser();
        if (user.getBranchId() == null) {
            return false;
        }
        return employeeRepository.findById(employeeId)
                .map(employee -> user.getBranchId().equals(employee.getBranchId()))
                .orElse(false);
    }

    public boolean isEmployeeOfMyAgent(Long employeeId) {
        User user = currentUser();
        if (user.getAgentId() == null) {
            return false;
        }
        return employeeRepository.findById(employeeId)
                .map(employee -> user.getAgentId().equals(employee.getAgentId()))
                .orElse(false);
    }

    // Looks up the payslip's actual owner from the database. Fails closed (false)
    // for an unknown payslipId, so an employee can never read someone else's payslip by id.
    public boolean isOwnPayslip(Long payslipId) {
        User user = currentUser();
        if (user.getEmployeeId() == null) {
            return false;
        }
        return payslipRepository.findById(payslipId)
                .map(payslip -> user.getEmployeeId().equals(payslip.getEmployeeId()))
                .orElse(false);
    }

    // An agent may open the payslip of one of their OWN employees, never anyone else's. Looks the
    // owner up in the database and fails closed (false) for an unknown id.
    public boolean isPayslipOfMyAgent(Long payslipId) {
        User user = currentUser();
        if (user.getAgentId() == null) {
            return false;
        }
        return payslipRepository.findById(payslipId)
                .flatMap(payslip -> employeeRepository.findById(payslip.getEmployeeId()))
                .map(employee -> user.getAgentId().equals(employee.getAgentId()))
                .orElse(false);
    }

    // The leave request's employee must work in the caller's branch — otherwise a Branch Manager
    // could decide another branch's leave just by guessing an id. Fails closed for an unknown id.
    public boolean isLeaveRequestInMyBranch(Long leaveRequestId) {
        User user = currentUser();
        if (user.getBranchId() == null) {
            return false;
        }
        return leaveRequestRepository.findById(leaveRequestId)
                .flatMap(leave -> employeeRepository.findById(leave.getEmployeeId()))
                .map(employee -> user.getBranchId().equals(employee.getBranchId()))
                .orElse(false);
    }

    // A Branch Manager may assign a shift to ONE of their own employees or to their whole branch.
    // A department spans every branch, so assigning by department is not theirs to do.
    public boolean canAssignShift(ShiftAssignmentRequest request) {
        if (request == null) {
            return false;
        }
        if (request.employeeId() != null) {
            return isEmployeeInMyBranch(request.employeeId());
        }
        if (request.branchId() != null) {
            return isSameBranch(request.branchId());
        }
        return false;
    }

    public Long currentBranchId() {
        return currentUser().getBranchId();
    }

    public Long currentAgentId() {
        return currentUser().getAgentId();
    }

    // FIX: this used to return the USER id, so every "my ..." endpoint (profile, attendance,
    // leave, loans) looked up the wrong employee whenever user.id != employee.id.
    public Long currentEmployeeId() {
        return currentUser().getEmployeeId();
    }

    public Long currentUserId() {
        return currentUser().getId();
    }
}