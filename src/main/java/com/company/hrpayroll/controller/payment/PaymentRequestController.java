// controller/payment/PaymentRequestController.java
package com.company.hrpayroll.controller.payment;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.payment.PaymentRejectionRequest;
import com.company.hrpayroll.dto.request.payment.PaymentReleaseRequest;
import com.company.hrpayroll.dto.request.payment.PaymentRequestCreateRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.payment.PaymentRequestResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.payment.PaymentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payment-requests")
@RequiredArgsConstructor
public class PaymentRequestController {

    private final PaymentRequestService paymentRequestService;
    private final AccessGuard accessGuard;

    // Who may SEE every agent's payment requests.
    private static final String STAFF_READ_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_OFFICER', 'PAYROLL_OFFICER', 'AUDITOR')";

    // An agent asks the Admin for money to pay their employees.
    @PostMapping
    @PreAuthorize("hasRole('AGENT')")
    @Auditable(action = "PAYMENT_REQUESTED", entityType = "PaymentRequest")
    public ApiResponse<PaymentRequestResponse> submit(@Valid @RequestBody PaymentRequestCreateRequest request) {
        return ApiResponse.success("Payment request sent to the Administrator",
                paymentRequestService.submit(request, accessGuard.currentAgentId(), accessGuard.currentUserId()));
    }

    // An agent withdraws their own request while it is still waiting for the Administrator.
    @PutMapping("/{paymentRequestId}/cancel")
    @PreAuthorize("hasRole('AGENT')")
    @Auditable(action = "PAYMENT_REQUEST_CANCELLED", entityType = "PaymentRequest")
    public ApiResponse<PaymentRequestResponse> cancel(@PathVariable Long paymentRequestId) {
        return ApiResponse.success("Payment request cancelled",
                paymentRequestService.cancel(paymentRequestId, accessGuard.currentAgentId()));
    }

    // Approving and rejecting are the Admin's call (Super Admin's powers belong to Admin).
    @PutMapping("/{paymentRequestId}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "PAYMENT_REQUEST_APPROVED", entityType = "PaymentRequest")
    public ApiResponse<PaymentRequestResponse> approve(@PathVariable Long paymentRequestId) {
        return ApiResponse.success("Payment request approved",
                paymentRequestService.approve(paymentRequestId, accessGuard.currentUserId()));
    }

    @PutMapping("/{paymentRequestId}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Auditable(action = "PAYMENT_REQUEST_REJECTED", entityType = "PaymentRequest")
    public ApiResponse<PaymentRequestResponse> reject(
            @PathVariable Long paymentRequestId,
            @Valid @RequestBody PaymentRejectionRequest request
    ) {
        return ApiResponse.success("Payment request rejected",
                paymentRequestService.reject(paymentRequestId, request.reason(), accessGuard.currentUserId()));
    }

    // The Admin sends the money and records how.
    @PutMapping("/{paymentRequestId}/release")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'FINANCE_OFFICER')")
    @Auditable(action = "PAYMENT_REQUEST_RELEASED", entityType = "PaymentRequest")
    public ApiResponse<PaymentRequestResponse> release(
            @PathVariable Long paymentRequestId,
            @Valid @RequestBody PaymentReleaseRequest request
    ) {
        return ApiResponse.success("Money released to the agent",
                paymentRequestService.release(paymentRequestId, request.reference(), accessGuard.currentUserId()));
    }

    // Company-wide, newest first. Optional ?status=PENDING|APPROVED|REJECTED|RELEASED|CANCELLED.
    @GetMapping
    @PreAuthorize(STAFF_READ_ROLES)
    public ApiResponse<List<PaymentRequestResponse>> getAll(@RequestParam(required = false) String status) {
        return ApiResponse.success(paymentRequestService.getAll(status));
    }

    @GetMapping("/{paymentRequestId}")
    @PreAuthorize(STAFF_READ_ROLES)
    public ApiResponse<PaymentRequestResponse> getOne(@PathVariable Long paymentRequestId) {
        return ApiResponse.success(paymentRequestService.getById(paymentRequestId));
    }

    // Agent Portal convenience endpoint — same /my pattern as the other agent modules.
    @GetMapping("/my")
    @PreAuthorize("hasRole('AGENT')")
    public ApiResponse<List<PaymentRequestResponse>> getMine() {
        return ApiResponse.success(paymentRequestService.getForAgent(accessGuard.currentAgentId()));
    }
}
