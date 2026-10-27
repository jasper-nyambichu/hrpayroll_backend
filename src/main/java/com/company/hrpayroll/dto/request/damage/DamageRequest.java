// dto/request/damage/DamageRequest.java
package com.company.hrpayroll.dto.request.damage;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// damageDate is optional: when omitted the damage is dated today (company time).
public record DamageRequest(
        @NotNull(message = "Employee is required") Long employeeId,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must be at most 500 characters") String description,
        @PastOrPresent(message = "Damage date cannot be in the future") LocalDate damageDate
) {}
