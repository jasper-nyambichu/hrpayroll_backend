package com.company.hrpayroll.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// Sent by the bridge-agent, authenticated with a device token. terminalUserId
// is the raw id the ZKTeco terminal reports for the scan — the backend
// resolves it to a real employeeId via BiometricDeviceMapping, rather than
// trusting a pre-resolved id from the agent. verifyMethod is left as a plain
// String (not an enum) until we've seen real device output and know the
// actual set of values a live terminal reports.
public record BiometricSyncRequest(
        @NotBlank(message = "Event ID is required") String eventId,
        @NotBlank(message = "Terminal user id is required") String terminalUserId,
        @NotBlank(message = "Device serial is required") String deviceSerial,
        @NotNull(message = "Timestamp is required") LocalDateTime timestamp,
        @NotBlank(message = "Verify method is required") String verifyMethod, // e.g. "FINGERPRINT"
        @NotBlank(message = "Event type is required") String eventType // CLOCK_IN or CLOCK_OUT
) {}