// dto/response/attendance/BiometricDeviceResponse.java
package com.company.hrpayroll.dto.response.attendance;

import java.time.LocalDateTime;

public record BiometricDeviceResponse(
        Long id,
        Long branchId,
        String branchName,
        String deviceSerial,
        LocalDateTime lastSyncAt,
        String status
) {}