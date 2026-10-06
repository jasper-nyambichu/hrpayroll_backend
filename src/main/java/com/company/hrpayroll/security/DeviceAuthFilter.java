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
import java.nio.charset.StandardCharsets;
import java.util.Optional;

// Protects /attendance/biometric/** — permitAll() at the Spring Security level (a kiosk has no
// user JWT), so this filter is the only thing standing between the endpoint and the internet.
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
            reject(response, "Missing device credentials");
            return;
        }

        Optional<BiometricDevice> deviceOpt = biometricDeviceRepository.findByDeviceSerial(deviceSerial);

        boolean valid = deviceOpt.isPresent()
                && "ACTIVE".equals(deviceOpt.get().getStatus())
                && passwordEncoder.matches(deviceToken, deviceOpt.get().getDeviceTokenHash());

        if (!valid) {
            reject(response, "Invalid device credentials");
            return;
        }

        request.setAttribute(DEVICE_SERIAL_ATTRIBUTE, deviceSerial);
        filterChain.doFilter(request, response);
    }

    // A JSON body (not sendError) so the error isn't swallowed by the secured /error dispatch.
    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }
}