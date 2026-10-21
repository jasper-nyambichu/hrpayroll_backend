// controller/attendance/EnrollmentController.java
package com.company.hrpayroll.controller.attendance;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.attendance.EnrollmentRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.service.attendance.BiometricEnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Enrollment itself is done by the Back Office officer (or a Branch Manager) standing at the
// reader with the employee present, so it's a normal JWT-protected staff endpoint —
// it's the SYNC endpoint (below) that's device-token authenticated, not this one.
@RestController
@RequestMapping("/attendance/enrollment")
@RequiredArgsConstructor
public class EnrollmentController {

    private final BiometricEnrollmentService biometricEnrollmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'BRANCH_MANAGER', 'BACK_OFFICE')")
    @Auditable(action = "BIOMETRIC_TERMINAL_MAPPED", entityType = "BiometricDeviceMapping")
    public ApiResponse<Void> enrollMapping(@Valid @RequestBody EnrollmentRequest request) {
        biometricEnrollmentService.enrollMapping(request);
        return ApiResponse.success("Terminal user mapped to employee", null);
    }
}