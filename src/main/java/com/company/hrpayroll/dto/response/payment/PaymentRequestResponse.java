// dto/response/payment/PaymentRequestResponse.java
package com.company.hrpayroll.dto.response.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// paidTotal / remaining / paymentCount come from the M-Pesa payments recorded against the request,
// so the agent (and the Accounts Office) can see how much of the released money is still unpaid.
public record PaymentRequestResponse(
        Long id,
        Long agentId,
        String agentName,
        BigDecimal amount,
        String purpose,
        String status,
        String requestedByName,
        LocalDateTime requestedAt,
        String decidedByName,
        LocalDateTime decidedAt,
        String rejectionReason,
        String releasedByName,
        LocalDateTime releasedAt,
        String releaseReference,
        BigDecimal paidTotal,
        BigDecimal remaining,
        int paymentCount
) {}
