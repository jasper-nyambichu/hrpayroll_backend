package com.company.hrpayroll.dto.request.attendance;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A branch PC's report: its reader plugged in or not, its app version, and scans it still holds. */
public record DeviceHeartbeatRequest(
        @NotNull Boolean readerConnected,
        @Size(max = 30) String appVersion,
        @Min(0) Integer pendingScans
) {}
