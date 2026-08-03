// dto/response/shift/ShiftResponse.java
package com.company.hrpayroll.dto.response.shift;

import com.company.hrpayroll.entity.shift.ShiftType;

import java.time.LocalTime;

public record ShiftResponse(
        Long id,
        String name,
        ShiftType type,
        LocalTime startTime,
        LocalTime endTime
) {}