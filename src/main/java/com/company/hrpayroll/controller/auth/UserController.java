// controller/auth/UserController.java
package com.company.hrpayroll.controller.auth;

import com.company.hrpayroll.audit.AuditDetails;
import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.auth.ChangePasswordRequest;
import com.company.hrpayroll.dto.request.auth.CreateUserRequest;
import com.company.hrpayroll.dto.request.auth.CredentialsRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.auth.SavedLoginResponse;
import com.company.hrpayroll.dto.response.auth.UserSummaryResponse;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.service.audit.AuditService;
import com.company.hrpayroll.service.auth.UserService;
import com.company.hrpayroll.service.branch.BranchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final com.company.hrpayroll.service.auth.StaffFingerprintService staffFingerprintService;
    private final UserService userService;
    private final AuditService auditService;
    private final BranchService branchService;

    // The signed-in account as it stands now, so a screen can tell if the Admin has just changed it.
    @GetMapping("/me")
    public ApiResponse<UserSummaryResponse> me(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.success(userService.findById(currentUser.getId()));
    }

    // Only Super Admin can create staff/agent/employee login accounts —
    // matches "the only role that can create Admin accounts" from the proposal.
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "USER_CREATED", entityType = "User")
    public ApiResponse<UserSummaryResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created", userService.createUser(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<List<UserSummaryResponse>> findAllUsers() {
        return ApiResponse.success(userService.findAllUsers());
    }

    // A user can change their own password; Super Admin can act on any account.
    @PutMapping("/{userId}/change-password")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(userId, request);
        return ApiResponse.success("Password changed", null);
    }

    // The Admin sets a login's username and password and shares them. The audit trail never records the body.
    @PostMapping("/branches/{branchId}/login")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "USER_CREDENTIALS_CREATED", entityType = "User")
    public ApiResponse<UserSummaryResponse> createBranchLogin(
            @PathVariable Long branchId,
            @RequestParam Role role,
            @Valid @RequestBody CredentialsRequest request
    ) {
        return ApiResponse.success("Login created", userService.createBranchLogin(branchId, role, request));
    }

    @PostMapping("/agents/{agentId}/login")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "USER_CREDENTIALS_CREATED", entityType = "User")
    public ApiResponse<UserSummaryResponse> createAgentLogin(
            @PathVariable Long agentId,
            @Valid @RequestBody CredentialsRequest request
    ) {
        return ApiResponse.success("Login created", userService.createAgentLogin(agentId, request));
    }

    @PutMapping("/{userId}/credentials")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "USER_CREDENTIALS_CHANGED", entityType = "User")
    public ApiResponse<UserSummaryResponse> changeCredentials(
            @PathVariable Long userId,
            @Valid @RequestBody CredentialsRequest request
    ) {
        return ApiResponse.success("Login changed", userService.changeCredentials(userId, request));
    }

    // The saved username and password of a login the Admin set, to share it again. Every look is audited.
    @GetMapping("/{userId}/saved-login")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "USER_CREDENTIALS_VIEWED", entityType = "User")
    public ApiResponse<SavedLoginResponse> savedLogin(@PathVariable Long userId) {
        return ApiResponse.success(userService.savedLogin(userId));
    }

    // The employee record of the person behind a Branch Manager or Back Office login (null to unlink).
    // It marks them in the branch PC's lists and lets them sign in there with their fingerprint.
    public record UserEmployeeRequest(Long employeeId) {}

    @PutMapping("/{userId}/employee")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "USER_EMPLOYEE_LINKED", entityType = "User")
    public ApiResponse<UserSummaryResponse> linkEmployee(@PathVariable Long userId, @RequestBody UserEmployeeRequest request) {
        return ApiResponse.success(request.employeeId() == null ? "Employee unlinked" : "Employee linked",
                staffFingerprintService.linkEmployee(userId, request.employeeId()));
    }

    // Which branch a Branch Manager or Back Office login works at, which decides what they can see.
    public record UserBranchRequest(@NotNull(message = "Branch is required") Long branchId, Boolean swap) {}

    @PutMapping("/{userId}/branch")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserSummaryResponse> assignBranch(
            @PathVariable Long userId,
            @Valid @RequestBody UserBranchRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        Long from = userService.findById(userId).branchId();
        String fromName = from == null ? null : branchService.getBranchById(from).name();
        String toName = branchService.getBranchById(request.branchId()).name();
        UserService.BranchMove move = userService.assignBranch(userId, request.branchId(), Boolean.TRUE.equals(request.swap()));

        // Logged by hand rather than with @Auditable, so the history says where they moved from and to.
        recordBranchMove(currentUser, userId, fromName, toName);
        if (move.swappedWith() != null) {
            recordBranchMove(currentUser, move.swappedWith().id(), toName, fromName);
        }
        return ApiResponse.success("Branch saved", move.moved());
    }

    private void recordBranchMove(User admin, Long userId, String fromBranch, String toBranch) {
        auditService.record(admin.getId(), "USER_BRANCH_ASSIGNED", "User", userId,
                AuditDetails.withRequest("fromBranch", fromBranch, "toBranch", toBranch));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "USER_DELETED", entityType = "User")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId, @AuthenticationPrincipal User currentUser) {
        userService.deleteUser(userId, currentUser.getId());
        return ApiResponse.success("User deleted", null);
    }

    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "USER_DEACTIVATED", entityType = "User")
    public ApiResponse<Void> deactivateUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
        return ApiResponse.success("User deactivated", null);
    }
}