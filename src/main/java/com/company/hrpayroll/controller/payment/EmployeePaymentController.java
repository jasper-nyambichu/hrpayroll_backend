// controller/payment/EmployeePaymentController.java
package com.company.hrpayroll.controller.payment;

import com.company.hrpayroll.audit.Auditable;
import com.company.hrpayroll.dto.request.payment.EmployeePaymentRequest;
import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.payment.EmployeePaymentResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.payment.EmployeePaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee-payments")
@RequiredArgsConstructor
public class EmployeePaymentController {

    private final EmployeePaymentService employeePaymentService;
    private final AccessGuard accessGuard;

    private static final String STAFF_READ_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_OFFICER', 'PAYROLL_OFFICER', 'AUDITOR')";

    // The agent records a payment they made to one of their employees by M-Pesa.
    @PostMapping
    @PreAuthorize("hasRole('AGENT')")
    @Auditable(action = "EMPLOYEE_PAYMENT_RECORDED", entityType = "EmployeePayment")
    public ApiResponse<EmployeePaymentResponse> record(@Valid @RequestBody EmployeePaymentRequest request) {
        return ApiResponse.success("Payment recorded",
                employeePaymentService.record(request, accessGuard.currentAgentId(), accessGuard.currentUserId()));
    }

    // The agent's own payments, newest first. Optional ?paymentRequestId=.
    @GetMapping("/my")
    @PreAuthorize("hasRole('AGENT')")
    public ApiResponse<List<EmployeePaymentResponse>> getMine(@RequestParam(required = false) Long paymentRequestId) {
        return ApiResponse.success(employeePaymentService.getForAgent(accessGuard.currentAgentId(), paymentRequestId));
    }

    // Staff view across every agent. Optional ?paymentRequestId= and ?employeeId=.
    @GetMapping
    @PreAuthorize(STAFF_READ_ROLES)
    public ApiResponse<List<EmployeePaymentResponse>> getAll(
            @RequestParam(required = false) Long paymentRequestId,
            @RequestParam(required = false) Long employeeId
    ) {
        return ApiResponse.success(employeePaymentService.getAll(paymentRequestId, employeeId));
    }
}
