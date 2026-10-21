// service/auth/impl/AuthServiceImpl.java
package com.company.hrpayroll.service.auth.impl;

import com.company.hrpayroll.audit.AuditDetails;
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
import com.company.hrpayroll.service.audit.AuditService;
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

    // Audit trail actions for sign-in (proposal §5: "a record of every login").
    private static final String ACTION_SUCCESS = "LOGIN_SUCCESS";
    private static final String ACTION_FAILED = "LOGIN_FAILED";
    private static final String ACTION_BLOCKED = "LOGIN_BLOCKED";

    private static final String SURFACE_STAFF = "staff";
    private static final String SURFACE_EMPLOYEE = "employee";
    private static final String SURFACE_AGENT = "agent";

    // Every role that signs in on the staff login surface: everyone except AGENT and
    // EMPLOYEE, which have their own dedicated endpoints below and must never authenticate
    // here even with a correct password. Derived (not listed), so a role added later is
    // staff by default.
    private static final Set<Role> STAFF_ROLES = EnumSet.complementOf(EnumSet.of(Role.AGENT, Role.EMPLOYEE));

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;

    @Override
    public AuthResponse login(LoginRequest request) {
        String identifier = request.username();
        String key = "login:" + identifier;
        guardAgainstLockout(key, SURFACE_STAFF, identifier);

        User user = userRepository.findByUsername(identifier)
                .orElseThrow(() -> fail(key, SURFACE_STAFF, identifier, null));

        if (!STAFF_ROLES.contains(user.getRole())) {
            throw fail(key, SURFACE_STAFF, identifier, user.getId());
        }

        verify(key, SURFACE_STAFF, identifier, user, request.password());
        loginAttemptService.recordSuccess(key);
        auditLogin(user.getId(), ACTION_SUCCESS, SURFACE_STAFF, identifier, null);
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse employeeLogin(EmployeeLoginRequest request) {
        String identifier = request.employeeNumber();
        String key = "employee:" + identifier;
        guardAgainstLockout(key, SURFACE_EMPLOYEE, identifier);

        Employee employee = employeeRepository.findByEmployeeNumber(identifier)
                .orElseThrow(() -> fail(key, SURFACE_EMPLOYEE, identifier, null));

        User user = userRepository.findByEmployeeId(employee.getId())
                .orElseThrow(() -> fail(key, SURFACE_EMPLOYEE, identifier, null));

        if (user.getRole() != Role.EMPLOYEE) {
            throw fail(key, SURFACE_EMPLOYEE, identifier, user.getId());
        }

        verify(key, SURFACE_EMPLOYEE, identifier, user, request.pin());
        loginAttemptService.recordSuccess(key);
        auditLogin(user.getId(), ACTION_SUCCESS, SURFACE_EMPLOYEE, identifier, null);
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse agentLogin(AgentLoginRequest request) {
        String identifier = request.agentCode();
        String key = "agent:" + identifier;
        guardAgainstLockout(key, SURFACE_AGENT, identifier);

        User user = userRepository.findByUsername(identifier)
                .orElseThrow(() -> fail(key, SURFACE_AGENT, identifier, null));

        if (user.getRole() != Role.AGENT || user.getAgentId() == null) {
            throw fail(key, SURFACE_AGENT, identifier, user.getId());
        }

        verify(key, SURFACE_AGENT, identifier, user, request.password());
        loginAttemptService.recordSuccess(key);
        auditLogin(user.getId(), ACTION_SUCCESS, SURFACE_AGENT, identifier, null);
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

    private void guardAgainstLockout(String key, String surface, String identifier) {
        if (loginAttemptService.isLocked(key)) {
            auditLogin(null, ACTION_BLOCKED, surface, identifier, "ACCOUNT_LOCKED");
            throw new AccountLockedException(LOCKED_MESSAGE);
        }
    }

    /** Records the failure against the lockout key and in the audit trail, and returns the exception to throw. */
    private InvalidCredentialsException fail(String key, String surface, String identifier, Long userId) {
        loginAttemptService.recordFailure(key);
        auditLogin(userId, ACTION_FAILED, surface, identifier, "INVALID_CREDENTIALS");
        return new InvalidCredentialsException(GENERIC_FAILURE);
    }

    private void verify(String key, String surface, String identifier, User user, String rawCredential) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), rawCredential)
            );
        } catch (AuthenticationException ex) {
            throw fail(key, surface, identifier, user.getId());
        }
    }

    // The attempted identifier is recorded (a username, employee number or agent code);
    // the password / PIN never is. The write is independent of the login's outcome.
    private void auditLogin(Long userId, String action, String surface, String identifier, String reason) {
        auditService.record(userId, action, "User", userId,
                AuditDetails.withRequest("surface", surface, "identifier", identifier, "reason", reason));
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