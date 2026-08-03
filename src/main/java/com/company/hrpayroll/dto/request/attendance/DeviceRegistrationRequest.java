// dto/request/attendance/DeviceRegistrationRequest.java
package com.company.hrpayroll.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceRegistrationRequest(
        @NotNull(message = "Branch is required") Long branchId,
        @NotBlank(message = "Device serial is required") String deviceSerial
) {}