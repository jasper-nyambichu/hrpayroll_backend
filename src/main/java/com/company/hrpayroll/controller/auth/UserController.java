// controller/auth/UserController.java
package com.company.hrpayroll.controller.auth;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.auth.ChangePasswordRequest;
import com.company.hrpayroll.dto.request.auth.CreateUserRequest;
import com.company.hrpayroll.dto.request.auth.CredentialsRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.auth.UserSummaryResponse;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.service.auth.UserService;
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

    private final UserService userService;

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

    public record RunPaymentsRequest(@NotNull(message = "Choose yes or no") Boolean allowed) {}

    // Lets a Branch Manager run the agent payment calculation while the Admin is away, or takes it back.
    @PutMapping("/{userId}/run-payments")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "USER_RUN_PAYMENTS_CHANGED", entityType = "User")
    public ApiResponse<UserSummaryResponse> setRunPayments(
            @PathVariable Long userId,
            @Valid @RequestBody RunPaymentsRequest request
    ) {
        return ApiResponse.success("Saved", userService.setRunPayments(userId, request.allowed()));
    }

    // Which branch a Back Office officer works at, which also limits what their dashboard shows.
    // Branch Managers are not moved here because their branch decides what they can see.
    public record UserBranchRequest(@NotNull(message = "Branch is required") Long branchId) {}

    @PutMapping("/{userId}/branch")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "USER_BRANCH_ASSIGNED", entityType = "User")
    public ApiResponse<UserSummaryResponse> assignBranch(
            @PathVariable Long userId,
            @Valid @RequestBody UserBranchRequest request
    ) {
        return ApiResponse.success("Branch saved", userService.assignBackOfficeBranch(userId, request.branchId()));
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