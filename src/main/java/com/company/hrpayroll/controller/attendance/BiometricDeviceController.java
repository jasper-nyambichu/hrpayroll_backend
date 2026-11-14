// controller/attendance/BiometricDeviceController.java
package com.company.hrpayroll.controller.attendance;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.attendance.DeviceRegistrationRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.attendance.BiometricDeviceResponse;
import com.company.hrpayroll.dto.response.attendance.DeviceRegistrationResponse;
import com.company.hrpayroll.service.attendance.BiometricDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Registering hardware is for the Admin and, for their own branch only, the Back Office officer who
// runs the desktop application — this is the step that mints a device token, which is why it's a JWT-protected
// endpoint even though the device itself later authenticates differently.
@RestController
@RequestMapping("/attendance/devices")
@RequiredArgsConstructor
public class BiometricDeviceController {

    private final BiometricDeviceService biometricDeviceService;

    @PostMapping
    // The Back Office or the Branch Manager activates a PC of their own branch; every PC of a branch can be activated.
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasAnyRole('BACK_OFFICE', 'BRANCH_MANAGER') and @accessGuard.isSameBranch(#request.branchId()))")
    @Auditable(action = "BIOMETRIC_DEVICE_REGISTERED", entityType = "BiometricDevice")
    public ApiResponse<DeviceRegistrationResponse> registerDevice(
            @Valid @RequestBody DeviceRegistrationRequest request
    ) {
        return ApiResponse.success(
                "Device registered — copy the token into the bridge-agent config now, it will not be shown again",
                biometricDeviceService.registerDevice(request)
        );
    }

    @GetMapping("/by-branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or @accessGuard.isSameBranch(#branchId)")
    public ApiResponse<List<BiometricDeviceResponse>> getDevicesByBranch(@PathVariable Long branchId) {
        return ApiResponse.success(biometricDeviceService.getDevicesByBranch(branchId));
    }

    @PutMapping("/{deviceId}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasAnyRole('BACK_OFFICE', 'BRANCH_MANAGER') and @accessGuard.isDeviceInMyBranch(#deviceId))")
    @Auditable(action = "BIOMETRIC_DEVICE_DEACTIVATED", entityType = "BiometricDevice")
    public ApiResponse<Void> deactivateDevice(@PathVariable Long deviceId) {
        biometricDeviceService.deactivateDevice(deviceId);
        return ApiResponse.success("Device deactivated", null);
    }
}