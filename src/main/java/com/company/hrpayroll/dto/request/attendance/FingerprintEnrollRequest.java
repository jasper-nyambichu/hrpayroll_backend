package com.company.hrpayroll.dto.request.attendance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Sent by the desktop application when it enrols a finger, and to back up a template it already holds.
public record FingerprintEnrollRequest(
        @NotNull(message = "Employee is required") Long employeeId,
        @NotBlank(message = "Device serial is required") String deviceSerial,
        @NotBlank(message = "Terminal user id is required") @Size(max = 20) String terminalUserId,
        @NotNull(message = "Finger is required") @Min(0) @Max(10) Short fingerSlot,
        @NotBlank(message = "Template is required") @Size(max = 4096) String templateData, // base64
        @Min(0) @Max(100) Integer qualityScore
) {}
