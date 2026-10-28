// dto/response/payment/EmployeePaymentResponse.java
package com.company.hrpayroll.dto.response.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeePaymentResponse(
        Long id,
        Long paymentRequestId,
        Long agentId,
        Long employeeId,
        String employeeName,
        String employeeNumber,
        BigDecimal amount,
        String mpesaCode,
        LocalDate paidOn,
        String note,
        String recordedByName,
        LocalDateTime recordedAt
) {}
