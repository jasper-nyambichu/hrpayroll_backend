// dto/request/damage/BiometricConsentRequest.java
package com.company.hrpayroll.dto.request.damage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

// Sent by the desktop application / bridge agent when an employee places their finger to agree to a
// damage. The terminal matches the fingerprint on-device and reports only WHICH terminal user
// matched — the backend never sees fingerprint data. eventId makes a retried delivery harmless;
// occurredAt is the exact UTC instant of the scan (optional — the server's time is used if absent).
public record BiometricConsentRequest(
        @NotBlank(message = "Event id is required") String eventId,
        @NotBlank(message = "Device serial is required") String deviceSerial,
        @NotBlank(message = "Terminal user id is required") String terminalUserId,
        @NotNull(message = "Damage is required") Long damageId,
        Instant occurredAt
) {}
