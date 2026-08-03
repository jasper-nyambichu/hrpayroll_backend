// dto/request/attendance/EnrollmentRequest.java
package com.company.hrpayroll.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(
        @NotNull(message = "Employee is required") Long employeeId,
        @NotBlank(message = "Device serial is required") String deviceSerial,
        @NotNull(message = "Finger index is required") Integer fingerIndex,
        @NotBlank(message = "Template data is required") String templateDataBase64,
        Double qualityScore
) {}