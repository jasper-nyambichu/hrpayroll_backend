// dto/request/offday/OffDayUpdateRequest.java
package com.company.hrpayroll.dto.request.offday;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Moves one off day to another date (and/or changes its reason). */
public record OffDayUpdateRequest(
        @NotNull(message = "Choose the day off") LocalDate date,
        @Size(max = 255, message = "Reason cannot exceed 255 characters") String reason
) {}
