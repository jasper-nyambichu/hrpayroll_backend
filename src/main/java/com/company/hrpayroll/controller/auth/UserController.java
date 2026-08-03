// controller/auth/UserController.java
package com.company.hrpayroll.controller.auth;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.auth.ChangePasswordRequest;
import com.company.hrpayroll.dto.request.auth.CreateUserRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.auth.UserSummaryResponse;
import com.company.hrpayroll.service.auth.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "USER_DEACTIVATED", entityType = "User")
    public ApiResponse<Void> deactivateUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
        return ApiResponse.success("User deactivated", null);
    }
}