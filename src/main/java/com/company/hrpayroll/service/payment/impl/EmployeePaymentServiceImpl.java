// service/payment/impl/EmployeePaymentServiceImpl.java
package com.company.hrpayroll.service.payment.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.payment.EmployeePaymentRequest;
import com.company.hrpayroll.dto.response.payment.EmployeePaymentResponse;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.payment.EmployeePayment;
import com.company.hrpayroll.entity.payment.PaymentRequest;
import com.company.hrpayroll.exception.DuplicateResourceException;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.payment.PaymentMapper;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.payment.EmployeePaymentRepository;
import com.company.hrpayroll.repository.payment.PaymentRequestRepository;
import com.company.hrpayroll.service.notification.NotificationRecipients;
import com.company.hrpayroll.service.payment.EmployeePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeePaymentServiceImpl implements EmployeePaymentService {

    private final EmployeePaymentRepository employeePaymentRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final PaymentMapper paymentMapper;
    private final NotificationRecipients recipients;
    private final BusinessClock businessClock;

    @Override
    @Transactional
    public EmployeePaymentResponse record(EmployeePaymentRequest request, Long agentId, Long recordedByUserId) {
        PaymentRequest paymentRequest = paymentRequestRepository.findById(request.paymentRequestId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment request not found with id: " + request.paymentRequestId()));

        // Someone else's request looks exactly like one that does not exist.
        if (!paymentRequest.getAgentId().equals(agentId)) {
            throw new ResourceNotFoundException("Payment request not found with id: " + request.paymentRequestId());
        }

        if (!"RELEASED".equals(paymentRequest.getStatus())) {
            throw new InvalidOperationException(
                    "The money for this request has not been released yet. Payments can only be recorded "
                            + "once the Administrator releases it.");
        }

        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId()));

        if (!agentId.equals(employee.getAgentId())) {
            throw new InvalidOperationException("You can only record payments for employees you supply");
        }
        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new InvalidOperationException("Payments can only be recorded for active employees");
        }

        String mpesaCode = request.mpesaCode().trim().toUpperCase();
        if (employeePaymentRepository.existsByMpesaCode(mpesaCode)) {
            throw new DuplicateResourceException("The M-Pesa code " + mpesaCode + " has already been recorded");
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal alreadyPaid = employeePaymentRepository.findByPaymentRequestId(paymentRequest.getId()).stream()
                .map(EmployeePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = paymentRequest.getAmount().subtract(alreadyPaid);

        if (amount.compareTo(remaining) > 0) {
            throw new InvalidOperationException(
                    "That is more than the " + String.format(Locale.US, "KES %,.2f", remaining)
                            + " still unpaid on this request");
        }

        LocalDate paidOn = request.paidOn() != null ? request.paidOn() : businessClock.today();
        EmployeePayment saved = employeePaymentRepository.save(
                paymentMapper.toEmployeePayment(request, agentId, recordedByUserId, mpesaCode, paidOn));

        return paymentMapper.toEmployeePaymentResponse(
                saved, employee.getFullName(), employee.getEmployeeNumber(), recipients.nameOf(recordedByUserId));
    }

    @Override
    public List<EmployeePaymentResponse> getForAgent(Long agentId, Long paymentRequestId) {
        List<EmployeePayment> payments = paymentRequestId != null
                ? employeePaymentRepository.findByAgentIdAndPaymentRequestId(agentId, paymentRequestId)
                : employeePaymentRepository.findByAgentId(agentId);
        return toResponses(payments);
    }

    @Override
    public List<EmployeePaymentResponse> getAll(Long paymentRequestId, Long employeeId) {
        List<EmployeePayment> payments;
        if (paymentRequestId != null) {
            payments = employeePaymentRepository.findByPaymentRequestId(paymentRequestId);
            if (employeeId != null) {
                payments = payments.stream().filter(p -> employeeId.equals(p.getEmployeeId())).toList();
            }
        } else if (employeeId != null) {
            payments = employeePaymentRepository.findByEmployeeId(employeeId);
        } else {
            payments = employeePaymentRepository.findAll();
        }
        return toResponses(payments);
    }

    // ---- helpers ----

    private List<EmployeePaymentResponse> toResponses(List<EmployeePayment> payments) {
        List<Long> employeeIds = payments.stream().map(EmployeePayment::getEmployeeId).distinct().toList();
        Map<Long, Employee> employees = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));

        return payments.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(payment -> {
                    Employee employee = employees.get(payment.getEmployeeId());
                    return paymentMapper.toEmployeePaymentResponse(
                            payment,
                            employee != null ? employee.getFullName() : null,
                            employee != null ? employee.getEmployeeNumber() : null,
                            recipients.nameOf(payment.getRecordedByUserId()));
                })
                .toList();
    }
}
