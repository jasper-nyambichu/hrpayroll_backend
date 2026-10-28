// service/payment/EmployeePaymentService.java
package com.company.hrpayroll.service.payment;

import com.company.hrpayroll.dto.request.payment.EmployeePaymentRequest;
import com.company.hrpayroll.dto.response.payment.EmployeePaymentResponse;

import java.util.List;

public interface EmployeePaymentService {

    // The agent records a payment they made by M-Pesa. Only against a RELEASED request of their own,
    // only for one of their own employees, never above what is still unpaid, and the M-Pesa code must
    // not have been recorded before.
    EmployeePaymentResponse record(EmployeePaymentRequest request, Long agentId, Long recordedByUserId);

    // The agent's own payments, newest first; the request filter is optional.
    List<EmployeePaymentResponse> getForAgent(Long agentId, Long paymentRequestId);

    // Staff view across every agent; both filters are optional.
    List<EmployeePaymentResponse> getAll(Long paymentRequestId, Long employeeId);
}
