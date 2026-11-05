package com.company.hrpayroll.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;

// Sent by the kiosk/bridge-agent, authenticated with a device token.
// timestamp = wall-clock time on the device (kept for older agents).
// timestampUtc = the exact instant (optional). When present the server converts it to the
// company timezone, so a PC with a wrong Windows timezone can't shift attendance times.
public record BiometricSyncRequest(
        @NotBlank(message = "Event ID is required") String eventId,
        @NotBlank(message = "Terminal user id is required") String terminalUserId,
        @NotBlank(message = "Device serial is required") String deviceSerial,
        @NotNull(message = "Timestamp is required") LocalDateTime timestamp,
        @NotBlank(message = "Verify method is required") String verifyMethod, // e.g. "FINGERPRINT"
        @NotBlank(message = "Event type is required") String eventType,        // CLOCK_IN, CLOCK_OUT, LUNCH_OUT, EMERGENCY_OUT or BREAK_RETURN
        Instant timestampUtc,
        Instant expectedBackAtUtc,                                             // EMERGENCY_OUT: when they said they would return
        String reason                                                          // EMERGENCY_OUT: why they are leaving
) {}