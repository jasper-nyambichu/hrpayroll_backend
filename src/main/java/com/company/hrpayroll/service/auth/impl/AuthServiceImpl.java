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
import com.company.hrpayroll.exception.InvalidCredentialsException;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.security.CustomUserDetailsService;
import com.company.hrpayroll.security.JwtService;
import com.company.hrpayroll.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * One message for every failure on every surface. Distinguishing "no such
     * employee number" from "wrong PIN" would hand an attacker a free
     * enumeration oracle over a 10,000-key PIN space.
     */
    private static final String GENERIC_FAILURE = "Invalid credentials";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException(GENERIC_FAILURE));

        verify(user.getUsername(), request.password());
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse employeeLogin(EmployeeLoginRequest request) {
        Employee employee = employeeRepository.findByEmployeeNumber(request.employeeNumber())
                .orElseThrow(() -> new InvalidCredentialsException(GENERIC_FAILURE));

        User user = userRepository.findByEmployeeId(employee.getId())
                .orElseThrow(() -> new InvalidCredentialsException(GENERIC_FAILURE));

        verify(user.getUsername(), request.pin());
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse agentLogin(AgentLoginRequest request) {
        // The agent code is the username on the AGENT-role account linked to
        // that agent — see AgentLoginRequest for why there is no code column.
        User user = userRepository.findByUsername(request.agentCode())
                .orElseThrow(() -> new InvalidCredentialsException(GENERIC_FAILURE));

        if (user.getRole() != Role.AGENT || user.getAgentId() == null) {
            throw new InvalidCredentialsException(GENERIC_FAILURE);
        }

        verify(user.getUsername(), request.password());
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

    /**
     * Runs the credential through the same AuthenticationManager on all three
     * surfaces, so BCrypt matching, the active/locked checks on the User entity
     * and any future authentication rules apply uniformly. Every
     * AuthenticationException — bad password, disabled, locked — collapses to
     * one 401; without this a deactivated account would surface as a 500.
     */
    private void verify(String username, String rawCredential) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, rawCredential)
            );
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException(GENERIC_FAILURE);
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
