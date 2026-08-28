// dto/request/attendance/EnrollmentRequest.java
package com.company.hrpayroll.dto.request.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Enrollment now happens physically at the ZKTeco terminal — the employee
// scans directly on the device. This request just records the mapping
// between the terminal's internal user id and our real employee id, sent
// by the bridge-agent (or entered by HR) once that's known.
public record EnrollmentRequest(
        @NotNull(message = "Employee is required") Long employeeId,
        @NotBlank(message = "Device serial is required") String deviceSerial,
        @NotBlank(message = "Terminal user id is required") String terminalUserId
) {}