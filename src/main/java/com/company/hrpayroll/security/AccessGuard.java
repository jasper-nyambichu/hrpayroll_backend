// security/AccessGuard.java
package com.company.hrpayroll.security;

import com.company.hrpayroll.dto.request.shift.ShiftAssignmentRequest;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.repository.attendance.AttendanceRepository;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import com.company.hrpayroll.repository.damage.DamageRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.leave.LeaveRequestRepository;
import com.company.hrpayroll.repository.settlement.AgentSettlementRepository;
import com.company.hrpayroll.repository.settlement.EmployeePayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Used inside @PreAuthorize expressions across every module, e.g.
// @PreAuthorize("hasRole('SUPER_ADMIN') or @accessGuard.isSameBranch(#branchId)")
@Component("accessGuard")
@RequiredArgsConstructor
public class AccessGuard {

    private final AttendanceRepository attendanceRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final DamageRepository damageRepository;
    private final EmployeeRepository employeeRepository;
    private final AgentSettlementRepository agentSettlementRepository;
    private final EmployeePayslipRepository employeePayslipRepository;
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

    // The record's own branch, not the employee's: it stays where the day was worked. Fails closed for an unknown id.
    public boolean isAttendanceInMyBranch(Long attendanceId) {
        return attendanceRepository.findById(attendanceId)
                .map(attendance -> isSameBranch(attendance.getBranchId()))
                .orElse(false);
    }

    // A reader belongs to one branch. Fails closed for an unknown id.
    public boolean isDeviceInMyBranch(Long deviceId) {
        return biometricDeviceRepository.findById(deviceId)
                .map(device -> isSameBranch(device.getBranchId()))
                .orElse(false);
    }

    // A damage stays with the branch it was recorded at, and is also open to the branch the employee works at
    // now, because only that branch's reader can take their consent. Fails closed for an unknown id.
    public boolean isDamageInMyBranch(Long damageId) {
        return damageRepository.findById(damageId)
                .map(damage -> isSameBranch(damage.getBranchId()) || isEmployeeInMyBranch(damage.getEmployeeId()))
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

    // An agent may open only their OWN payment statement. Looks the owner up in the database and
    // fails closed (false) for an unknown id.
    public boolean isSettlementOfMyAgent(Long settlementId) {
        User user = currentUser();
        if (user.getAgentId() == null) {
            return false;
        }
        return agentSettlementRepository.findById(settlementId)
                .map(settlement -> user.getAgentId().equals(settlement.getAgentId()))
                .orElse(false);
    }

    // A Branch Manager may open the payslips of employees in their own branch. Fails closed for an unknown id.
    public boolean isPayslipInMyBranch(Long payslipId) {
        return employeePayslipRepository.findById(payslipId)
                .map(payslip -> isEmployeeInMyBranch(payslip.getEmployeeId()))
                .orElse(false);
    }

    // An agent may open the payslips of a payment they were given. Fails closed for an unknown id.
    public boolean isPayslipOfMyAgent(Long payslipId) {
        return employeePayslipRepository.findById(payslipId)
                .map(payslip -> isSettlementOfMyAgent(payslip.getSettlementId()))
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

    public boolean isAgent() {
        return currentUser().getRole() == Role.AGENT;
    }

    public boolean isBranchManager() {
        return currentUser().getRole() == Role.BRANCH_MANAGER;
    }

    public boolean isBackOffice() {
        return currentUser().getRole() == Role.BACK_OFFICE;
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