// dto/request/offday/OffDayRequest.java
package com.company.hrpayroll.dto.request.offday;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Gives an employee single days off: any dates, e.g. the 3rd and the 16th, never a range. An employee has at most
 * two off days in a calendar month, so a request carries one or two dates.
 */
public record OffDayRequest(
        @NotNull(message = "Employee is required") Long employeeId,
        @NotEmpty(message = "Choose the days off")
        @Size(max = 2, message = "An employee has at most 2 off days in a month")
        List<@NotNull(message = "Choose the days off") LocalDate> dates,
        @Size(max = 255, message = "Reason cannot exceed 255 characters") String reason
) {}
