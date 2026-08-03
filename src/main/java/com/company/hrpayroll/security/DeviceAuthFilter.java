// security/DeviceAuthFilter.java
package com.company.hrpayroll.security;

import com.company.hrpayroll.entity.attendance.BiometricDevice;
import com.company.hrpayroll.repository.attendance.BiometricDeviceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

// Protects /attendance/biometric/** — these routes are permitAll() at the
// Spring Security level (a bridge-agent has no user JWT), so this filter is
// the only thing standing between an open endpoint and a real device check.
@Component
@RequiredArgsConstructor
public class DeviceAuthFilter extends OncePerRequestFilter {

    private static final String SERIAL_HEADER = "X-Device-Serial";
    private static final String TOKEN_HEADER = "X-Device-Token";
    public static final String DEVICE_SERIAL_ATTRIBUTE = "authenticatedDeviceSerial";

    private final BiometricDeviceRepository biometricDeviceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/attendance/biometric")) {
            filterChain.doFilter(request, response);
            return;
        }

        String deviceSerial = request.getHeader(SERIAL_HEADER);
        String deviceToken = request.getHeader(TOKEN_HEADER);

        if (deviceSerial == null || deviceToken == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing device credentials");
            return;
        }

        Optional<BiometricDevice> deviceOpt = biometricDeviceRepository.findByDeviceSerial(deviceSerial);

        boolean valid = deviceOpt.isPresent()
                && "ACTIVE".equals(deviceOpt.get().getStatus())
                && passwordEncoder.matches(deviceToken, deviceOpt.get().getDeviceTokenHash());

        if (!valid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid device credentials");
            return;
        }

        request.setAttribute(DEVICE_SERIAL_ATTRIBUTE, deviceSerial);
        filterChain.doFilter(request, response);
    }
}