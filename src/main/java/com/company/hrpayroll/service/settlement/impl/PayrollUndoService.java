package com.company.hrpayroll.service.settlement.impl;

import com.company.hrpayroll.entity.agentadvance.AgentAdvance;
import com.company.hrpayroll.entity.damage.Damage;
import com.company.hrpayroll.entity.loan.Loan;
import com.company.hrpayroll.entity.loan.LoanRepayment;
import com.company.hrpayroll.entity.settlement.AgentSettlement;
import com.company.hrpayroll.entity.settlement.SettlementAdvanceRecovery;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.exception.SettlementInProgressException;
import com.company.hrpayroll.repository.agentadvance.AgentAdvanceRepository;
import com.company.hrpayroll.repository.damage.DamageRepository;
import com.company.hrpayroll.repository.loan.LoanRepaymentRepository;
import com.company.hrpayroll.repository.loan.LoanRepository;
import com.company.hrpayroll.repository.payment.EmployeePaymentRepository;
import com.company.hrpayroll.repository.payment.PaymentRequestRepository;
import com.company.hrpayroll.repository.settlement.AgentSettlementRepository;
import com.company.hrpayroll.repository.settlement.EmployeePayslipRepository;
import com.company.hrpayroll.repository.settlement.SettlementAdvanceRecoveryRepository;
import com.company.hrpayroll.service.notification.NotificationService;
import com.company.hrpayroll.service.settlement.SettlementLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Undoes one agent payment that was run by mistake (or as a test), as if it had never been run:
 *   - each loan instalment it took is given back (a loan it closed is open again) and its repayment row removed;
 *   - each damage it deducted is approved and waiting again;
 *   - each advance it took back is owed again (the advance itself, its request and approval stay as they were);
 *   - its payment request (Money Requests), payslips and the payment itself are removed, and the notices about it
 *     are cleared.
 * All in one transaction. Refused once the agent has recorded paying employees from it, since money really moved.
 */
@Service
@RequiredArgsConstructor
public class PayrollUndoService {

    private final AgentSettlementRepository settlementRepository;
    private final EmployeePayslipRepository payslipRepository;
    private final SettlementAdvanceRecoveryRepository recoveryRepository;
    private final AgentAdvanceRepository advanceRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final DamageRepository damageRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final EmployeePaymentRepository employeePaymentRepository;
    private final NotificationService notificationService;
    private final SettlementLockService lockService;

    /** What was undone, for the Admin's message. */
    public record UndoResult(Long settlementId, int loansRestored, int damagesRestored, int advancesRestored,
                             BigDecimal advanceAmountOwedAgain, int payslipsRemoved) {}

    @Transactional
    public UndoResult undo(Long settlementId) {
        if (!lockService.tryAcquire()) {
            throw new SettlementInProgressException("An agent payment calculation is in progress. Please try again shortly.");
        }
        AgentSettlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent payment not found with id: " + settlementId));

        Long requestId = settlement.getPaymentRequestId();
        if (requestId != null && !employeePaymentRepository.findByPaymentRequestId(requestId).isEmpty()) {
            throw new InvalidOperationException(
                    "The agent has already recorded paying employees from this payment, so it can't be undone.");
        }

        List<SettlementAdvanceRecovery> recoveries = recoveryRepository.findBySettlementIdOrderByIdAsc(settlementId);
        if (recoveries.isEmpty() && settlement.getAdvanceRecovered().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidOperationException(
                    "This payment was run before each advance was recorded separately, so the advances it took back can't be "
                            + "given back automatically. Ask for it to be corrected by hand.");
        }

        // Loans: give each instalment back.
        int loans = 0;
        for (LoanRepayment repayment : loanRepaymentRepository.findByPayrollRunId(settlementId)) {
            Loan loan = loanRepository.findById(repayment.getLoanId()).orElse(null);
            if (loan != null) {
                loan.setOutstandingBalance(loan.getOutstandingBalance().add(repayment.getAmountPaid()));
                if ("CLOSED".equals(loan.getStatus()) && loan.getOutstandingBalance().signum() > 0) {
                    loan.setStatus("APPROVED");
                }
                loanRepository.save(loan);
                loans++;
            }
            loanRepaymentRepository.delete(repayment);
        }

        // Damages: approved and waiting for a payment again.
        int damages = 0;
        for (Damage damage : damageRepository.findByPayrollRunIdIn(List.of(settlementId))) {
            damage.setStatus("APPROVED");
            damage.setPayrollRunId(null);
            damage.setDeductedAt(null);
            damageRepository.save(damage);
            damages++;
        }

        // Advances: owed again. The advance, its request and its approval stay as they were.
        BigDecimal owedAgain = BigDecimal.ZERO;
        for (SettlementAdvanceRecovery recovery : recoveries) {
            AgentAdvance advance = advanceRepository.findById(recovery.getAdvanceId()).orElse(null);
            if (advance != null) {
                BigDecimal balance = advance.getOutstandingBalance().add(recovery.getAmount()).min(advance.getAmount());
                advance.setOutstandingBalance(balance);
                advance.setStatus(balance.compareTo(advance.getAmount()) >= 0 ? "OUTSTANDING" : "PARTIALLY_RECOVERED");
                advanceRepository.save(advance);
                owedAgain = owedAgain.add(recovery.getAmount());
            }
        }
        recoveryRepository.deleteAll(recoveries);

        // The payslips, the money request and the payment itself, with the notices about them.
        // (The payment points at its money request, so it goes first.)
        var payslips = payslipRepository.findBySettlementIdIn(List.of(settlementId));
        payslipRepository.deleteAll(payslips);
        recoveryRepository.flush();
        payslipRepository.flush();
        notificationService.resolveByReference("AGENT_INVOICE_READY", settlementId);
        settlementRepository.delete(settlement);
        settlementRepository.flush();
        if (requestId != null) {
            for (String type : List.of("PAYMENT_REQUEST_APPROVED", "PAYMENT_REQUEST_RELEASED")) {
                notificationService.resolveByReference(type, requestId);
            }
            paymentRequestRepository.deleteById(requestId);
        }

        return new UndoResult(settlementId, loans, damages, recoveries.size(), owedAgain, payslips.size());
    }
}
