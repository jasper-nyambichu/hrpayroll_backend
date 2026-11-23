// service/payment/PaymentRequestService.java
package com.company.hrpayroll.service.payment;

import com.company.hrpayroll.dto.request.payment.PaymentRequestCreateRequest;
import com.company.hrpayroll.dto.response.payment.PaymentRequestResponse;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRequestService {

    // The agent comes from the signed-in account. Starts PENDING; every Admin is told.
    PaymentRequestResponse submit(PaymentRequestCreateRequest request, Long agentId, Long requestedByUserId);

    // A calculated agent payment needs no decision: it starts APPROVED, ready for the Admin to release.
    Long createApproved(Long agentId, BigDecimal amount, String purpose, Long adminUserId);

    // An agent can withdraw their own request while it is still PENDING.
    PaymentRequestResponse cancel(Long paymentRequestId, Long agentId);

    // Admin: PENDING -> APPROVED (ready to release) or REJECTED (with a reason).
    PaymentRequestResponse approve(Long paymentRequestId, Long adminUserId);
    PaymentRequestResponse reject(Long paymentRequestId, String reason, Long adminUserId);

    // Admin: APPROVED -> RELEASED, keeping how the money was sent as the proof.
    PaymentRequestResponse release(Long paymentRequestId, String reference, Long releasedByUserId);

    // Company-wide, newest first; status is optional (PENDING, APPROVED, REJECTED, RELEASED, CANCELLED).
    List<PaymentRequestResponse> getAll(String status);
    List<PaymentRequestResponse> getForAgent(Long agentId);
    PaymentRequestResponse getById(Long paymentRequestId);

    // The Accounts Office pays an approved agent invoice and records how (M-Pesa, cheque or manual) with proof.
    PaymentRequestResponse pay(Long paymentRequestId, com.company.hrpayroll.dto.request.payment.AgentPaymentRecord record,
                               org.springframework.web.multipart.MultipartFile evidence, Long paidByUserId);

    // The proof uploaded with a payment. agentId: when an agent asks, only their own payment's.
    com.company.hrpayroll.dto.response.common.GeneratedDocument evidence(Long paymentRequestId, Long agentId);
}
