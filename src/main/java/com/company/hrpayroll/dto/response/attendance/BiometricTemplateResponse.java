// dto/response/attendance/BiometricTemplateResponse.java
package com.company.hrpayroll.dto.response.attendance;

import java.time.LocalDateTime;

// Metadata only — never includes the raw template bytes. General API
// consumers should never see template data, only the sync channel below.
public record BiometricTemplateResponse(
        Long id,
        Long employeeId,
        Integer fingerIndex,
        Double qualityScore,
        LocalDateTime enrolledAt
) {}