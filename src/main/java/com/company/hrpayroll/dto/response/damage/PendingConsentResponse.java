// dto/response/damage/PendingConsentResponse.java
package com.company.hrpayroll.dto.response.damage;

import java.math.BigDecimal;
import java.time.LocalDate;

// Pulled by the desktop application: the damages waiting for an employee enrolled on THIS device
// to agree by fingerprint. terminalUserId tells the application which scan means which damage.
public record PendingConsentResponse(
        Long damageId,
        String terminalUserId,
        Long employeeId,
        String employeeName,
        String employeeNumber,
        BigDecimal amount,
        String description,
        LocalDate damageDate
) {}
