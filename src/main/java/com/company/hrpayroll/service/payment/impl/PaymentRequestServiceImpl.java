// service/payment/impl/PaymentRequestServiceImpl.java
package com.company.hrpayroll.service.payment.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.payment.AgentPaymentRecord;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.entity.payment.PaymentEvidence;
import com.company.hrpayroll.exception.DuplicateResourceException;
import com.company.hrpayroll.repository.payment.PaymentEvidenceRepository;
import com.company.hrpayroll.service.company.AccountsOffice;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private static final String TYPE_APPROVED = "PAYMENT_REQUEST_APPROVED";
    private static final String TYPE_REJECTED = "PAYMENT_REQUEST_REJECTED";
    private static final String TYPE_RELEASED = "PAYMENT_REQUEST_RELEASED";
    private static final String TYPE_PAID = "AGENT_PAYMENT_PAID";          // -> the Admins, once Accounts has paid
    public static final String TYPE_TO_PAY = "AGENT_INVOICE_TO_PAY";      // -> the Accounts Office, after a payroll run

    private static final long MAX_EVIDENCE_BYTES = 5L * 1024 * 1024;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final PaymentRequestRepository paymentRequestRepository;
    private final EmployeePaymentRepository employeePaymentRepository;
    private final AgentRepository agentRepository;
    private final PaymentMapper paymentMapper;
    private final NotificationService notificationService;
    private final NotificationRecipients recipients;
    private final BusinessClock businessClock;
    private final PaymentEvidenceRepository evidenceRepository;
    private final AccountsOffice accountsOffice;
    private final com.company.hrpayroll.repository.agentadvance.AgentAdvanceRepository advanceRepository;

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
    public Long createApproved(Long agentId, BigDecimal amount, String purpose, Long adminUserId) {
        PaymentRequest saved = paymentRequestRepository.save(PaymentRequest.builder()
                .agentId(agentId)
                .amount(amount)
                .purpose(purpose)
                .status(APPROVED)
                .requestedByUserId(adminUserId)
                .decidedByUserId(adminUserId)
                .decidedAt(businessClock.now())
                .build());

        recipients.agentLogin(agentId).ifPresent(agentUser ->
                notificationService.notifyInApp(agentUser.getId(), TYPE_APPROVED,
                        purpose + ": " + kes(amount) + ". " + whoPays(), saved.getId()));

        return saved.getId();
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

        recipients.agentLogin(saved.getAgentId()).ifPresent(agentUser ->
                notificationService.notifyInApp(agentUser.getId(), TYPE_APPROVED,
                        "Your request for " + kes(saved.getAmount())
                                + " was approved. " + whoPays(), saved.getId()));

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
        // Where the organisation has an Accounts Office, it pays (with the mode of payment and proof), not the Admin.
        if (accountsOffice.enabled()) {
            throw new InvalidOperationException(
                    "The Accounts Office pays agents here, recording how it was paid (M-Pesa, cheque or cash).");
        }
        PaymentRequest request = findOrThrow(paymentRequestId);
        requireStatus(request, APPROVED, "released");

        request.setStatus(RELEASED);
        request.setReleaseReference(reference.trim());
        request.setReleasedByUserId(releasedByUserId);
        request.setReleasedAt(businessClock.now());
        PaymentRequest saved = paymentRequestRepository.save(request);

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

    @Override
    @Transactional
    public PaymentRequestResponse pay(Long paymentRequestId, AgentPaymentRecord record, MultipartFile evidence, Long paidByUserId) {
        accountsOffice.requireEnabled();
        PaymentRequest request = paymentRequestRepository.findForUpdate(paymentRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment request not found with id: " + paymentRequestId));
        requireStatus(request, APPROVED, "paid");

        LocalDate today = businessClock.today();
        LocalDate paidOn = record.paidOn() == null ? today : record.paidOn();
        if (paidOn.isAfter(today)) {
            throw new InvalidOperationException("The payment date cannot be in the future");
        }

        String method = text(record.method()).toUpperCase(Locale.ROOT);
        String summary;
        switch (method) {
            case "MPESA" -> {
                String code = text(record.reference()).toUpperCase(Locale.ROOT);
                if (!code.matches("^[A-Z0-9]{10}$")) {
                    throw new InvalidOperationException("An M-Pesa code is 10 letters and numbers, e.g. QGH7K2L9XP");
                }
                if (paymentRequestRepository.existsByPaymentMethodAndReleaseReferenceIgnoreCase("MPESA", code)) {
                    throw new DuplicateResourceException("The M-Pesa code " + code + " has already been used for another payment");
                }
                request.setReleaseReference(code);
                summary = "M-Pesa " + code;
            }
            case "CHEQUE" -> {
                String number = text(record.chequeNumber()).toUpperCase(Locale.ROOT);
                String bank = text(record.chequeBank());
                if (!number.matches("^[A-Z0-9-]{4,30}$")) {
                    throw new InvalidOperationException("Enter the cheque number as printed on the cheque");
                }
                if (bank.length() < 2 || bank.length() > 100) {
                    throw new InvalidOperationException("Enter the bank the cheque is drawn on");
                }
                if (record.chequeDate() == null) {
                    throw new InvalidOperationException("Enter the date written on the cheque");
                }
                if (paymentRequestRepository.existsByPaymentMethodAndChequeBankIgnoreCaseAndChequeNumberIgnoreCase("CHEQUE", bank, number)) {
                    throw new DuplicateResourceException("Cheque " + number + " of " + bank + " has already been used for another payment");
                }
                request.setChequeNumber(number);
                request.setChequeBank(bank);
                request.setChequeDate(record.chequeDate());
                summary = clip("Cheque " + number + " · " + bank + " · dated " + record.chequeDate().format(DAY), 100);
                request.setReleaseReference(summary);
            }
            case "MANUAL" -> {
                String reference = text(record.reference());
                if (reference.length() < 3) {
                    throw new InvalidOperationException("Enter the cash receipt number or reference");
                }
                request.setReleaseReference(clip(reference, 100));
                summary = "Cash, receipt " + clip(reference, 100);
            }
            default -> throw new InvalidOperationException("Choose how the agent was paid: M-Pesa, cheque or cash");
        }

        String note = text(record.note());
        if (note.length() > 500) {
            throw new InvalidOperationException("The note can be at most 500 characters");
        }

        request.setPaymentMethod(method);
        request.setPaidOn(paidOn);
        request.setPaymentNote(note.isEmpty() ? null : note);
        request.setStatus(RELEASED);
        request.setReleasedByUserId(paidByUserId);
        request.setReleasedAt(businessClock.now());
        PaymentRequest saved = paymentRequestRepository.save(request);

        if (evidence != null && !evidence.isEmpty()) {
            saveEvidence(saved.getId(), evidence, paidByUserId);
        }

        String agent = agentName(saved.getAgentId());
        String when = paidOn.format(DAY);
        String how = summary;
        recipients.agentLogin(saved.getAgentId()).ifPresent(agentUser ->
                notificationService.notifyInApp(agentUser.getId(), TYPE_RELEASED,
                        kes(saved.getAmount()) + " has been paid to you by the Accounts Office on " + when + " (" + how
                                + "). You can now record each M-Pesa payment to your employees.", saved.getId()));
        String toAdmins = "The Accounts Office paid " + agent + " " + kes(saved.getAmount()) + " on " + when
                + " (" + how + ") for " + saved.getPurpose() + ".";
        recipients.admins().forEach(admin ->
                notificationService.notifyInApp(admin.getId(), TYPE_PAID, toAdmins, saved.getId()));
        notificationService.resolveByReference(TYPE_TO_PAY, saved.getId());

        return toResponse(saved);
    }

    @Override
    public GeneratedDocument evidence(Long paymentRequestId, Long agentId) {
        PaymentRequest request = findOrThrow(paymentRequestId);
        // Another agent's payment looks exactly like one that does not exist.
        if (agentId != null && !agentId.equals(request.getAgentId())) {
            throw new ResourceNotFoundException("Payment request not found with id: " + paymentRequestId);
        }
        PaymentEvidence evidence = evidenceRepository.findByPaymentRequestId(paymentRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("No proof of payment was uploaded for this payment"));
        return new GeneratedDocument(evidence.getFileName(), evidence.getContentType(), evidence.getData());
    }

    private void saveEvidence(Long paymentRequestId, MultipartFile file, Long userId) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the uploaded proof of payment", e);
        }
        if (content.length > MAX_EVIDENCE_BYTES) {
            throw new InvalidOperationException("The proof of payment must be 5 MB or smaller");
        }
        // Going by the file's first bytes, not the name or type the browser claims it is.
        String contentType = detectType(content);
        if (contentType == null) {
            throw new InvalidOperationException("The proof of payment must be a PDF, PNG or JPEG file");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String name = original.replaceAll("[^A-Za-z0-9 ._()-]", "").trim();
        if (name.isEmpty()) {
            String extension = contentType.equals("application/pdf") ? ".pdf" : contentType.equals("image/png") ? ".png" : ".jpg";
            name = "proof-of-payment-" + paymentRequestId + extension;
        }
        evidenceRepository.save(PaymentEvidence.builder()
                .paymentRequestId(paymentRequestId)
                .fileName(clip(name, 200))
                .contentType(contentType)
                .data(content)
                .uploadedByUserId(userId)
                .build());
    }

    private static String detectType(byte[] c) {
        if (c.length >= 4 && c[0] == '%' && c[1] == 'P' && c[2] == 'D' && c[3] == 'F') return "application/pdf";
        if (c.length >= 4 && (c[0] & 0xFF) == 0x89 && c[1] == 'P' && c[2] == 'N' && c[3] == 'G') return "image/png";
        if (c.length >= 3 && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8 && (c[2] & 0xFF) == 0xFF) return "image/jpeg";
        return null;
    }

    private String whoPays() {
        return accountsOffice.enabled() ? "The Accounts Office will pay it." : "The Administrator will release the money.";
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String clip(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
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
                payments.size(),
                evidenceRepository.existsByPaymentRequestId(request.getId()),
                advanceRepository.findFirstByPaymentRequestId(request.getId()).map(a -> a.getId()).orElse(null));
    }

    private String agentName(Long agentId) {
        return agentRepository.findById(agentId).map(Agent::getName).orElse("the agent");
    }

    private static String kes(BigDecimal amount) {
        return String.format(Locale.US, "KES %,.2f", amount);
    }
}
