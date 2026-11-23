// dto/request/payment/AgentPaymentRecord.java
package com.company.hrpayroll.dto.request.payment;

import java.time.LocalDate;

/**
 * How the Accounts Office paid an agent invoice, with the proof (V39). Which fields are needed depends on the
 * method, so they are checked in PaymentRequestServiceImpl.pay:
 *   MPESA   reference = the M-Pesa transaction code (10 letters and numbers)
 *   CHEQUE  chequeNumber, chequeBank and chequeDate as written on the cheque
 *   MANUAL  reference = how it was paid (e.g. cash receipt number, bank transfer reference)
 * paidOn is the day the money went out (today if empty); note is optional.
 */
public record AgentPaymentRecord(
        String method,
        String reference,
        String chequeNumber,
        String chequeBank,
        LocalDate chequeDate,
        LocalDate paidOn,
        String note
) {}
