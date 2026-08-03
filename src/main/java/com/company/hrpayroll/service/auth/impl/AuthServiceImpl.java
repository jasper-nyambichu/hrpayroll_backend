// service/auth/impl/AuthServiceImpl.java
package com.company.hrpayroll.service.auth.impl;

import com.company.hrpayroll.dto.request.auth.LoginRequest;
import com.company.hrpayroll.dto.request.auth.RefreshTokenRequest;
import com.company.hrpayroll.dto.response.auth.AuthResponse;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.exception.InvalidCredentialsException;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.security.CustomUserDetailsService;
import com.company.hrpayroll.security.JwtService;
import com.company.hrpayroll.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        String username = jwtService.extractUsername(request.refreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(request.refreshToken(), userDetails)) {
            throw new InvalidCredentialsException("Refresh token is invalid or expired");
        }

        return buildAuthResponse((User) userDetails);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getRole().name(),
                user.getDisplayName(),
                user.isMustChangePassword()
        );
    }
}