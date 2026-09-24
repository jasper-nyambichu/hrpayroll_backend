// controller/attendance/BiometricSyncController.java
package com.company.hrpayroll.controller.attendance;

import com.company.hrpayroll.dto.request.attendance.BiometricSyncRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.attendance.AttendanceResponse;
import com.company.hrpayroll.dto.response.attendance.DeviceMappingItemResponse;
import com.company.hrpayroll.security.DeviceAuthFilter;
import com.company.hrpayroll.service.attendance.BiometricEnrollmentService;
import com.company.hrpayroll.service.attendance.BiometricSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Deliberately NO @PreAuthorize anywhere in this class — these routes are
// permitAll() at the SecurityConfig level and protected entirely by
// DeviceAuthFilter instead, which runs before this controller and rejects
// anything without a valid device token. A bridge-agent has no user JWT,
// so @PreAuthorize's role checks don't apply here at all.
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
        // The device serial DeviceAuthFilter already verified is available
        // here via the request attribute it set — used to sanity-check that
        // the request body's deviceSerial matches the one actually
        // authenticated, rather than trusting the body alone.
        String authenticatedSerial = (String) httpRequest.getAttribute(DeviceAuthFilter.DEVICE_SERIAL_ATTRIBUTE);
        if (!authenticatedSerial.equals(request.deviceSerial())) {
            return ApiResponse.error("Device serial mismatch between authentication and request body");
        }
        return ApiResponse.success(biometricSyncService.syncEvent(request));
    }

    // Pulled by the bridge-agent on startup and periodically thereafter to
    // refresh its local terminalUserId -> employeeId cache. Previously
    // called a non-existent getTemplatesForDevice() returning
    // TemplateSyncItemResponse — that method was never implemented on
    // BiometricEnrollmentService and didn't compile. Enrollment mapping
    // (not fingerprint template data) is what's actually stored server-side
    // now, so this calls the real method and returns the real type.
    @GetMapping("/templates")
    public ApiResponse<List<DeviceMappingItemResponse>> getMappingsForMyBranch(HttpServletRequest httpRequest) {
        String deviceSerial = (String) httpRequest.getAttribute(DeviceAuthFilter.DEVICE_SERIAL_ATTRIBUTE);
        return ApiResponse.success(biometricEnrollmentService.getMappingsForDevice(deviceSerial));
    }
}