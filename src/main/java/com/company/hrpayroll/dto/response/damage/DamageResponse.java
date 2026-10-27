// dto/response/damage/DamageResponse.java
package com.company.hrpayroll.dto.response.damage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DamageResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String employeeNumber,
        Long branchId,
        BigDecimal amount,
        String description,
        LocalDate damageDate,
        String status,
        Long recordedByUserId,
        String recordedByName,
        String approvalMethod,
        LocalDateTime consentAt,
        boolean overridden,
        String overrideReason,
        String overriddenByName,
        LocalDateTime overriddenAt,
        Long payrollRunId,
        LocalDateTime createdAt
) {}
