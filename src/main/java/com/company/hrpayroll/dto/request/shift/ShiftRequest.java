// dto/request/shift/ShiftRequest.java
package com.company.hrpayroll.dto.request.shift;

import com.company.hrpayroll.entity.shift.ShiftType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ShiftRequest(
        @NotBlank(message = "Shift name is required") String name,
        @NotNull(message = "Shift type is required") ShiftType type,
        LocalTime startTime,
        LocalTime endTime
) {}