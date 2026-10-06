// controller/attendance/BiometricSyncController.java
package com.company.hrpayroll.controller.attendance;

import com.company.hrpayroll.dto.request.attendance.BiometricSyncRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;
import com.company.hrpayroll.dto.response.attendance.DeviceMappingItemResponse;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.security.DeviceAuthFilter;
import com.company.hrpayroll.service.attendance.BiometricEnrollmentService;
import com.company.hrpayroll.service.attendance.BiometricSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// No @PreAuthorize here: these routes are permitAll() in SecurityConfig and protected entirely by
// DeviceAuthFilter (device serial + token headers). A kiosk has no user JWT.
@RestController
@RequestMapping("/attendance/biometric")
@RequiredArgsConstructor
public class BiometricSyncController {

    private final BiometricSyncService biometricSyncService;
    private final BiometricEnrollmentService biometricEnrollmentService;

    @PostMapping("/sync")
    public ApiResponse<AttendanceResponse> syncEvent(
            @Valid @RequestBody BiometricSyncRequest request,
            HttpServletRequest httpRequest
    ) {
        String authenticatedSerial = (String) httpRequest.getAttribute(DeviceAuthFilter.DEVICE_SERIAL_ATTRIBUTE);
        if (authenticatedSerial == null || !authenticatedSerial.equals(request.deviceSerial())) {
            // 400 (not 200 + success=false) so clients can classify it as a permanent rejection.
            throw new InvalidOperationException("Device serial mismatch between authentication and request body");
        }

        return ApiResponse.success(biometricSyncService.syncEvent(request));
    }

    // Pulled by the kiosk on startup and periodically: terminal user id -> employee (with name).
    @GetMapping("/mappings")
    public ApiResponse<List<DeviceMappingItemResponse>> getMappingsForMyBranch(HttpServletRequest httpRequest) {
        String deviceSerial = (String) httpRequest.getAttribute(DeviceAuthFilter.DEVICE_SERIAL_ATTRIBUTE);
        return ApiResponse.success(biometricEnrollmentService.getMappingsForDevice(deviceSerial));
    }
}