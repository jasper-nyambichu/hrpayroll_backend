// controller/auth/AuthController.java
package com.company.hrpayroll.controller.auth;

import com.company.hrpayroll.service.auth.impl.PasswordResetService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.company.hrpayroll.dto.request.auth.AgentLoginRequest;
import com.company.hrpayroll.dto.request.auth.EmployeeLoginRequest;
import com.company.hrpayroll.dto.request.auth.LoginRequest;
import com.company.hrpayroll.dto.request.auth.RefreshTokenRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.auth.AuthResponse;
import com.company.hrpayroll.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// Matches SecurityConfig's permitAll() on /auth/** — this is the one
// endpoint all three frontend login surfaces (staff/agent/employee) call.
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }

    @PostMapping("/employee-login")
    public ApiResponse<AuthResponse> employeeLogin(@Valid @RequestBody EmployeeLoginRequest request) {
        return ApiResponse.success("Login successful", authService.employeeLogin(request));
    }

    @PostMapping("/agent-login")
    public ApiResponse<AuthResponse> agentLogin(@Valid @RequestBody AgentLoginRequest request) {
        return ApiResponse.success("Login successful", authService.agentLogin(request));
    }

    public record ForgotPasswordRequest(@NotBlank(message = "Enter your username or email") @Size(max = 150) String identifier) {}

    public record VerifyCodeRequest(
            @NotBlank(message = "Enter your username or email") @Size(max = 150) String identifier,
            @NotBlank(message = "Enter the code") @Pattern(regexp = "^\\d{6}$", message = "The code has 6 digits") String code
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "This reset has expired. Please start again.") String resetToken,
            @NotBlank(message = "A new password is required")
            @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
            @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*$",
                    message = "Password needs an uppercase letter, a lowercase letter and a number")
            String newPassword
    ) {}

    public record ResetTokenResponse(String resetToken) {}

    public record PasswordResetResponse(String role) {}

    // Always the same answer, so this cannot be used to find out who has an account.
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestCode(request.identifier());
        return ApiResponse.success("If that account has an email address, a code is on its way", null);
    }

    @PostMapping("/verify-reset-code")
    public ApiResponse<ResetTokenResponse> verifyResetCode(@Valid @RequestBody VerifyCodeRequest request) {
        return ApiResponse.success(new ResetTokenResponse(passwordResetService.verifyCode(request.identifier(), request.code())));
    }

    @PostMapping("/reset-password")
    public ApiResponse<PasswordResetResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ApiResponse.success("Password changed",
                new PasswordResetResponse(passwordResetService.resetPassword(request.resetToken(), request.newPassword())));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("Token refreshed", authService.refresh(request));
    }
}