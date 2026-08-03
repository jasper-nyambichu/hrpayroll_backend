// controller/leave/LeaveController.java — corrected approveByHr(), plus one new dependency

package com.company.hrpayroll.controller.leave;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.leave.LeaveApprovalRequest;
import com.company.hrpayroll.dto.request.leave.LeaveRequestCreateDto;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.leave.LeaveBalanceResponse;
import com.company.hrpayroll.dto.response.leave.LeaveRequestResponse;
import com.company.hrpayroll.dto.response.leave.LeaveTypeResponse;
import com.company.hrpayroll.event.LeaveApprovedEvent;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.leave.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final AccessGuard accessGuard;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository; // added — needed to resolve Employee.id -> User.id

    @GetMapping("/types")
    public ApiResponse<List<LeaveTypeResponse>> getAllLeaveTypes() {
        return ApiResponse.success(leaveService.getAllLeaveTypes());
    }

    @PostMapping("/apply")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<LeaveRequestResponse> applyForLeave(@Valid @RequestBody LeaveRequestCreateDto request) {
        return ApiResponse.success(
                "Leave request submitted",
                leaveService.applyForLeave(accessGuard.currentEmployeeId(), request)
        );
    }

    @PutMapping("/{leaveRequestId}/approve-by-manager")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Auditable(action = "LEAVE_APPROVED_BY_MANAGER", entityType = "LeaveRequest")
    public ApiResponse<LeaveRequestResponse> approveByManager(
            @PathVariable Long leaveRequestId, @RequestBody LeaveApprovalRequest request
    ) {
        return ApiResponse.success(
                request.approved() ? "Forwarded to HR for final approval" : "Leave request rejected",
                leaveService.approveByManager(leaveRequestId, request)
        );
    }

    // Fixed: resolves the employee's actual User.id via UserRepository
    // before publishing, instead of passing the Employee.id straight through
    // as if it were a User.id. If no login account exists for this employee
    // (e.g. onboarding not finished), the event is skipped with a log
    // warning rather than silently notifying the wrong (or no) account.
    @PutMapping("/{leaveRequestId}/approve-by-hr")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'SUPER_ADMIN')")
    @Auditable(action = "LEAVE_APPROVED_BY_HR", entityType = "LeaveRequest")
    public ApiResponse<LeaveRequestResponse> approveByHr(
            @PathVariable Long leaveRequestId, @RequestBody LeaveApprovalRequest request
    ) {
        LeaveRequestResponse response = leaveService.approveByHr(leaveRequestId, request);

        if (request.approved()) {
            userRepository.findByEmployeeId(response.employeeId()).ifPresentOrElse(
                    user -> eventPublisher.publishEvent(new LeaveApprovedEvent(leaveRequestId, user.getId())),
                    () -> log.warn(
                            "Leave request {} approved but employee {} has no linked user account — notification skipped",
                            leaveRequestId, response.employeeId())
            );
        }

        return ApiResponse.success(request.approved() ? "Leave approved" : "Leave rejected", response);
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER') " +
            "or (hasRole('BRANCH_MANAGER') and @accessGuard.isEmployeeInMyBranch(#employeeId)) " +
            "or @accessGuard.isSameEmployee(#employeeId)")
    public ApiResponse<List<LeaveRequestResponse>> getLeaveHistory(@PathVariable Long employeeId) {
        return ApiResponse.success(leaveService.getLeaveHistoryForEmployee(employeeId));
    }

    @GetMapping("/pending/manager")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ApiResponse<List<LeaveRequestResponse>> getPendingManagerApprovals() {
        return ApiResponse.success(leaveService.getPendingManagerApprovals(accessGuard.currentBranchId()));
    }

    @GetMapping("/pending/hr")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'SUPER_ADMIN')")
    public ApiResponse<List<LeaveRequestResponse>> getPendingHrApprovals() {
        return ApiResponse.success(leaveService.getPendingHrApprovals());
    }

    @GetMapping("/my-balance")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ApiResponse<List<LeaveBalanceResponse>> getMyLeaveBalance() {
        return ApiResponse.success(leaveService.getLeaveBalances(accessGuard.currentEmployeeId()));
    }
}