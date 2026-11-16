// controller/attendance/EnrollmentController.java
package com.company.hrpayroll.controller.attendance;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.attendance.EnrollmentRequest;
import com.company.hrpayroll.dto.request.attendance.FingerprintEnrollRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.entity.auth.User;
import com.company.hrpayroll.service.attendance.BiometricEnrollmentService;
import com.company.hrpayroll.service.auth.StaffFingerprintService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final StaffFingerprintService staffFingerprintService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'BRANCH_MANAGER', 'BACK_OFFICE')")
    @Auditable(action = "BIOMETRIC_TERMINAL_MAPPED", entityType = "BiometricDeviceMapping")
    public ApiResponse<Void> enrollMapping(@Valid @RequestBody EnrollmentRequest request) {
        biometricEnrollmentService.enrollMapping(request);
        return ApiResponse.success("Terminal user mapped to employee", null);
    }

    // The desktop application saves each enrolled finger here, so every branch PC can recognise it.
    // The audit row records the facts of the request only, never the template.
    @PostMapping("/fingerprints")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'BRANCH_MANAGER', 'BACK_OFFICE')")
    @Auditable(action = "BIOMETRIC_FINGERPRINT_SAVED", entityType = "Employee")
    public ApiResponse<Void> saveFingerprint(@Valid @RequestBody FingerprintEnrollRequest request) {
        biometricEnrollmentService.saveFingerprint(request);
        return ApiResponse.success("Fingerprint saved", null);
    }

    // The signed-in Branch Manager or Back Office registers their own sign-in finger on their branch's PC. It signs
    // that login in on the branch PCs and is never attendance, so it works for a manager who is not an employee.
    public record LoginFingerprintRequest(
            @jakarta.validation.constraints.NotBlank String deviceSerial,
            @jakarta.validation.constraints.NotBlank String templateData,
            Integer qualityScore) {}

    public record LoginFingerprintStatus(boolean registered) {}

    @PostMapping("/my-fingerprint")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'BACK_OFFICE')")
    @Auditable(action = "LOGIN_FINGERPRINT_SAVED", entityType = "User")
    public ApiResponse<LoginFingerprintStatus> saveMyFingerprint(
            @Valid @RequestBody LoginFingerprintRequest request,
            @AuthenticationPrincipal User current) {
        staffFingerprintService.saveOwnLoginFingerprint(current, request.deviceSerial(), request.templateData(), request.qualityScore());
        return ApiResponse.success("Fingerprint saved", new LoginFingerprintStatus(true));
    }

    @GetMapping("/my-fingerprint")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER', 'BACK_OFFICE')")
    public ApiResponse<LoginFingerprintStatus> myFingerprint(@AuthenticationPrincipal User current) {
        return ApiResponse.success(new LoginFingerprintStatus(staffFingerprintService.hasLoginFingerprint(current.getId())));
    }

    // Whether this employee's fingerprint will follow them to another branch. Never returns the template.
    @GetMapping("/employees/{employeeId}/fingerprints/count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<Long> countFingerprints(@PathVariable Long employeeId) {
        return ApiResponse.success(biometricEnrollmentService.countFingerprints(employeeId));
    }
}