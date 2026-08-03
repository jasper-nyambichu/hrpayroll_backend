// dto/request/attendance/BiometricSyncRequest.java
package com.company.hrpayroll.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// Sent by the bridge-agent, authenticated with a device token — not a user JWT.
public record BiometricSyncRequest(
        @NotBlank(message = "Event ID is required") String eventId,
        @NotNull(message = "Employee is required") Long employeeId,
        @NotBlank(message = "Device serial is required") String deviceSerial,
        @NotNull(message = "Timestamp is required") LocalDateTime timestamp,
        @NotNull(message = "Match confidence score is required") Double matchConfidenceScore,
        @NotBlank(message = "Event type is required") String eventType // CLOCK_IN or CLOCK_OUT
) {}