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
}
