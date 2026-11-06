// dto/request/attendance/ManualCheckInRequest.java
package com.company.hrpayroll.dto.request.attendance;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** The employees a Branch Manager ticked on the kiosk's manual check-in list. */
public record ManualCheckInRequest(
        @NotEmpty(message = "Choose at least one employee")
        @Size(max = 500, message = "Too many employees in one request")
        List<Long> employeeIds
) {}
