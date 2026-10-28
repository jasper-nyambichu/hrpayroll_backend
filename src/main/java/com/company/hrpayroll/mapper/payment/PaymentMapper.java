// mapper/payment/PaymentMapper.java
package com.company.hrpayroll.mapper.payment;

import com.company.hrpayroll.dto.request.payment.EmployeePaymentRequest;
import com.company.hrpayroll.dto.request.payment.PaymentRequestCreateRequest;
import com.company.hrpayroll.dto.response.payment.EmployeePaymentResponse;
import com.company.hrpayroll.dto.response.payment.PaymentRequestResponse;
import com.company.hrpayroll.entity.payment.EmployeePayment;
import com.company.hrpayroll.entity.payment.PaymentRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class PaymentMapper {

    public PaymentRequest toPaymentRequest(PaymentRequestCreateRequest dto, Long agentId, Long requestedByUserId) {
        return PaymentRequest.builder()
                .agentId(agentId)
                .amount(dto.amount().setScale(2, RoundingMode.HALF_UP))
                .purpose(dto.purpose().trim())
                .status("PENDING")
                .requestedByUserId(requestedByUserId)
                .build();
    }

    // Names and totals live elsewhere, so the service resolves them and passes them in.
    public PaymentRequestResponse toPaymentRequestResponse(
            PaymentRequest request, String agentName, String requestedByName, String decidedByName,
            String releasedByName, BigDecimal paidTotal, int paymentCount) {
        return new PaymentRequestResponse(
                request.getId(),
                request.getAgentId(),
                agentName,
                request.getAmount(),
                request.getPurpose(),
                request.getStatus(),
                requestedByName,
                request.getCreatedAt(),
                decidedByName,
                request.getDecidedAt(),
                request.getRejectionReason(),
                releasedByName,
                request.getReleasedAt(),
                request.getReleaseReference(),
                paidTotal,
                request.getAmount().subtract(paidTotal),
                paymentCount
        );
    }

    public EmployeePayment toEmployeePayment(
            EmployeePaymentRequest dto, Long agentId, Long recordedByUserId, String mpesaCode, LocalDate paidOn) {
        return EmployeePayment.builder()
                .paymentRequestId(dto.paymentRequestId())
                .agentId(agentId)
                .employeeId(dto.employeeId())
                .amount(dto.amount().setScale(2, RoundingMode.HALF_UP))
                .mpesaCode(mpesaCode)
                .paidOn(paidOn)
                .note(dto.note() != null && !dto.note().isBlank() ? dto.note().trim() : null)
                .recordedByUserId(recordedByUserId)
                .build();
    }

    public EmployeePaymentResponse toEmployeePaymentResponse(
            EmployeePayment payment, String employeeName, String employeeNumber, String recordedByName) {
        return new EmployeePaymentResponse(
                payment.getId(),
                payment.getPaymentRequestId(),
                payment.getAgentId(),
                payment.getEmployeeId(),
                employeeName,
                employeeNumber,
                payment.getAmount(),
                payment.getMpesaCode(),
                payment.getPaidOn(),
                payment.getNote(),
                recordedByName,
                payment.getCreatedAt()
        );
    }
}
