// service/auth/impl/AuthServiceImpl.java
package com.company.hrpayroll.service.auth.impl;

import com.company.hrpayroll.dto.request.auth.AgentLoginRequest;
import com.company.hrpayroll.dto.request.auth.EmployeeLoginRequest;
import com.company.hrpayroll.dto.request.auth.LoginRequest;
import com.company.hrpayroll.dto.request.auth.RefreshTokenRequest;
import com.company.hrpayroll.dto.response.auth.AuthResponse;
import com.company.hrpayroll.entity.auth.Role;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.exception.AccountLockedException;
import com.company.hrpayroll.exception.InvalidCredentialsException;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.security.CustomUserDetailsService;
import com.company.hrpayroll.security.JwtService;
import com.company.hrpayroll.security.LoginAttemptService;
import com.company.hrpayroll.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String GENERIC_FAILURE = "Invalid credentials";
    private static final String LOCKED_MESSAGE =
            "Too many failed attempts. This account is locked for 15 minutes.";

    // Every role that should be able to reach the staff login surface.
    // AGENT and EMPLOYEE deliberately excluded — they have their own
    // dedicated endpoints below, and must never authenticate here even
    // with a correct password.
    private static final Set<Role> STAFF_ROLES = EnumSet.of(
            Role.SUPER_ADMIN, Role.ADMIN, Role.HR_MANAGER, Role.PAYROLL_OFFICER,
            Role.BRANCH_MANAGER, Role.FINANCE_OFFICER, Role.AUDITOR
    );

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    @Override
    public AuthResponse login(LoginRequest request) {
        String key = "login:" + request.username();
        guardAgainstLockout(key);

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> fail(key));

        if (!STAFF_ROLES.contains(user.getRole())) {
            throw fail(key);
        }

        verify(key, user.getUsername(), request.password());
        loginAttemptService.recordSuccess(key);
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse employeeLogin(EmployeeLoginRequest request) {
        String key = "employee:" + request.employeeNumber();
        guardAgainstLockout(key);

        Employee employee = employeeRepository.findByEmployeeNumber(request.employeeNumber())
                .orElseThrow(() -> fail(key));

        User user = userRepository.findByEmployeeId(employee.getId())
                .orElseThrow(() -> fail(key));

        if (user.getRole() != Role.EMPLOYEE) {
            throw fail(key);
        }

        verify(key, user.getUsername(), request.pin());
        loginAttemptService.recordSuccess(key);
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse agentLogin(AgentLoginRequest request) {
        String key = "agent:" + request.agentCode();
        guardAgainstLockout(key);

        User user = userRepository.findByUsername(request.agentCode())
                .orElseThrow(() -> fail(key));

        if (user.getRole() != Role.AGENT || user.getAgentId() == null) {
            throw fail(key);
        }

        verify(key, user.getUsername(), request.password());
        loginAttemptService.recordSuccess(key);
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        String username;
        try {
            username = jwtService.extractUsername(request.refreshToken());
        } catch (RuntimeException ex) {
            throw new InvalidCredentialsException("Refresh token is invalid or expired");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(request.refreshToken(), userDetails)) {
            throw new InvalidCredentialsException("Refresh token is invalid or expired");
        }

        return buildAuthResponse((User) userDetails);
    }

    private void guardAgainstLockout(String key) {
        if (loginAttemptService.isLocked(key)) {
            throw new AccountLockedException(LOCKED_MESSAGE);
        }
    }

    /** Records the failure against the lockout key and returns the exception to throw. */
    private InvalidCredentialsException fail(String key) {
        loginAttemptService.recordFailure(key);
        return new InvalidCredentialsException(GENERIC_FAILURE);
    }

    private void verify(String key, String username, String rawCredential) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, rawCredential)
            );
        } catch (AuthenticationException ex) {
            throw fail(key);
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getDisplayName(),
                user.getEmployeeId(),
                user.getAgentId(),
                user.getBranchId(),
                user.isMustChangePassword()
        );
    }
}