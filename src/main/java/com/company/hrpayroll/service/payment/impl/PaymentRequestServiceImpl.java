// service/payment/impl/PaymentRequestServiceImpl.java
package com.company.hrpayroll.service.payment.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.payment.PaymentRequestCreateRequest;
import com.company.hrpayroll.dto.response.payment.PaymentRequestResponse;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.payment.EmployeePayment;
import com.company.hrpayroll.entity.payment.PaymentRequest;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.payment.PaymentMapper;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.payment.EmployeePaymentRepository;
import com.company.hrpayroll.repository.payment.PaymentRequestRepository;
import com.company.hrpayroll.service.notification.NotificationRecipients;
import com.company.hrpayroll.service.notification.NotificationService;
import com.company.hrpayroll.service.payment.PaymentRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentRequestServiceImpl implements PaymentRequestService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String RELEASED = "RELEASED";
    private static final String CANCELLED = "CANCELLED";

    private static final String TYPE_SUBMITTED = "PAYMENT_REQUEST_SUBMITTED";
    private static final String TYPE_TO_RELEASE = "PAYMENT_REQUEST_TO_RELEASE";
    private static final String TYPE_APPROVED = "PAYMENT_REQUEST_APPROVED";
    private static final String TYPE_REJECTED = "PAYMENT_REQUEST_REJECTED";
    private static final String TYPE_RELEASED = "PAYMENT_REQUEST_RELEASED";

    private final PaymentRequestRepository paymentRequestRepository;
    private final EmployeePaymentRepository employeePaymentRepository;
    private final AgentRepository agentRepository;
    private final PaymentMapper paymentMapper;
    private final NotificationService notificationService;
    private final NotificationRecipients recipients;
    private final BusinessClock businessClock;

    @Override
    @Transactional
    public PaymentRequestResponse submit(PaymentRequestCreateRequest request, Long agentId, Long requestedByUserId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + agentId));

        // Money must not be requested for, or sent to, a deactivated agent.
        if ("INACTIVE".equals(agent.getStatus())) {
            throw new InvalidOperationException("An inactive agent cannot request payments");
        }

        PaymentRequest saved = paymentRequestRepository.save(
                paymentMapper.toPaymentRequest(request, agentId, requestedByUserId));

        String message = agent.getName() + " requested " + kes(saved.getAmount())
                + " to pay employees: " + saved.getPurpose();
        recipients.admins().forEach(admin ->
                notificationService.notifyInApp(admin.getId(), TYPE_SUBMITTED, message, saved.getId()));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentRequestResponse cancel(Long paymentRequestId, Long agentId) {
        PaymentRequest request = findOrThrow(paymentRequestId);

        // Someone else's request looks exactly like one that does not exist.
        if (!request.getAgentId().equals(agentId)) {
            throw new ResourceNotFoundException("Payment request not found with id: " + paymentRequestId);
        }
        if (!PENDING.equals(request.getStatus())) {
            throw new InvalidOperationException("Only a request that is still waiting for the Administrator can be cancelled");
        }

        request.setStatus(CANCELLED);
        PaymentRequest saved = paymentRequestRepository.save(request);

        notificationService.resolveByReference(TYPE_SUBMITTED, saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentRequestResponse approve(Long paymentRequestId, Long adminUserId) {
        PaymentRequest request = findOrThrow(paymentRequestId);
        requireStatus(request, PENDING, "approved");

        request.setStatus(APPROVED);
        request.setDecidedByUserId(adminUserId);
        request.setDecidedAt(businessClock.now());
        PaymentRequest saved = paymentRequestRepository.save(request);

        notificationService.resolveByReference(TYPE_SUBMITTED, saved.getId());

        String agentName = agentName(saved.getAgentId());
        recipients.agentLogin(saved.getAgentId()).ifPresent(agentUser ->
                notificationService.notifyInApp(agentUser.getId(), TYPE_APPROVED,
                        "Your request for " + kes(saved.getAmount())
                                + " was approved. The Accounts Office will release the money.", saved.getId()));

        // The Accounts Office now has something to release.
        recipients.accountsOffice().forEach(officer ->
                notificationService.notifyInApp(officer.getId(), TYPE_TO_RELEASE,
                        "Release " + kes(saved.getAmount()) + " to " + agentName + " — approved by the Administrator.",
                        saved.getId()));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentRequestResponse reject(Long paymentRequestId, String reason, Long adminUserId) {
        PaymentRequest request = findOrThrow(paymentRequestId);
        requireStatus(request, PENDING, "rejected");

        request.setStatus(REJECTED);
        request.setRejectionReason(reason.trim());
        request.setDecidedByUserId(adminUserId);
        request.setDecidedAt(businessClock.now());
        PaymentRequest saved = paymentRequestRepository.save(request);

        notificationService.resolveByReference(TYPE_SUBMITTED, saved.getId());
        recipients.agentLogin(saved.getAgentId()).ifPresent(agentUser ->
                notificationService.notifyInApp(agentUser.getId(), TYPE_REJECTED,
                        "Your request for " + kes(saved.getAmount()) + " was rejected: " + saved.getRejectionReason(),
                        saved.getId()));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentRequestResponse release(Long paymentRequestId, String reference, Long releasedByUserId) {
        PaymentRequest request = findOrThrow(paymentRequestId);
        requireStatus(request, APPROVED, "released");

        request.setStatus(RELEASED);
        request.setReleaseReference(reference.trim());
        request.setReleasedByUserId(releasedByUserId);
        request.setReleasedAt(businessClock.now());
        PaymentRequest saved = paymentRequestRepository.save(request);

        notificationService.resolveByReference(TYPE_TO_RELEASE, saved.getId());
        recipients.agentLogin(saved.getAgentId()).ifPresent(agentUser ->
                notificationService.notifyInApp(agentUser.getId(), TYPE_RELEASED,
                        kes(saved.getAmount()) + " has been released (reference " + saved.getReleaseReference()
                                + "). You can now record each M-Pesa payment to your employees.", saved.getId()));

        return toResponse(saved);
    }

    @Override
    public List<PaymentRequestResponse> getAll(String status) {
        List<PaymentRequest> requests = (status == null || status.isBlank())
                ? paymentRequestRepository.findAll()
                : paymentRequestRepository.findByStatus(status.trim().toUpperCase());

        return requests.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<PaymentRequestResponse> getForAgent(Long agentId) {
        return paymentRequestRepository.findByAgentId(agentId).stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PaymentRequestResponse getById(Long paymentRequestId) {
        return toResponse(findOrThrow(paymentRequestId));
    }

    // ---- helpers ----

    private void requireStatus(PaymentRequest request, String expected, String action) {
        if (!expected.equals(request.getStatus())) {
            throw new InvalidOperationException(
                    "This request is " + request.getStatus().toLowerCase().replace('_', ' ')
                            + " and can no longer be " + action);
        }
    }

    private PaymentRequest findOrThrow(Long id) {
        return paymentRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment request not found with id: " + id));
    }

    private PaymentRequestResponse toResponse(PaymentRequest request) {
        List<EmployeePayment> payments = employeePaymentRepository.findByPaymentRequestId(request.getId());
        BigDecimal paidTotal = payments.stream()
                .map(EmployeePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return paymentMapper.toPaymentRequestResponse(
                request,
                agentName(request.getAgentId()),
                recipients.nameOf(request.getRequestedByUserId()),
                recipients.nameOf(request.getDecidedByUserId()),
                recipients.nameOf(request.getReleasedByUserId()),
                paidTotal,
                payments.size());
    }

    private String agentName(Long agentId) {
        return agentRepository.findById(agentId).map(Agent::getName).orElse("the agent");
    }

    private static String kes(BigDecimal amount) {
        return String.format(Locale.US, "KES %,.2f", amount);
    }
}
