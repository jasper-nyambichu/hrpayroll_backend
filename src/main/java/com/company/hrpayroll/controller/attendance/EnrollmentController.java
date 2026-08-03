// controller/attendance/EnrollmentController.java
package com.company.hrpayroll.controller.attendance;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.attendance.EnrollmentRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.attendance.BiometricTemplateResponse;
import com.company.hrpayroll.service.attendance.BiometricEnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Enrollment itself is done by HR staff standing at the branch reader with
// the employee present, so it's a normal JWT-protected staff endpoint —
// it's the SYNC endpoint (below) that's device-token authenticated, not this one.
@RestController
@RequestMapping("/attendance/enrollment")
@RequiredArgsConstructor
public class EnrollmentController {

    private final BiometricEnrollmentService biometricEnrollmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'BRANCH_MANAGER')")
    @Auditable(action = "BIOMETRIC_TEMPLATE_ENROLLED", entityType = "BiometricTemplate")
    public ApiResponse<BiometricTemplateResponse> enrollTemplate(@Valid @RequestBody EnrollmentRequest request) {
        return ApiResponse.success("Fingerprint enrolled", biometricEnrollmentService.enrollTemplate(request));
    }
}