package com.company.hrpayroll.dto.response.attendance;

// Pulled by the bridge-agent on startup and periodically thereafter to
// refresh its local terminalUserId -> employeeId cache. No fingerprint
// data travels over this channel at all under the ZKTeco model.
public record DeviceMappingItemResponse(
        String terminalUserId,
        Long employeeId
) {}