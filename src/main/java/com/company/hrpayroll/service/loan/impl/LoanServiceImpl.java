// service/loan/impl/LoanServiceImpl.java
package com.company.hrpayroll.service.loan.impl;

import com.company.hrpayroll.config.BusinessClock;
import com.company.hrpayroll.dto.request.loan.LoanRequest;
import com.company.hrpayroll.dto.response.loan.LoanRepaymentResponse;
import com.company.hrpayroll.dto.response.loan.LoanResponse;
import com.company.hrpayroll.entity.employee.Employee;
import com.company.hrpayroll.entity.loan.Loan;
import com.company.hrpayroll.entity.loan.LoanRepayment;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.loan.LoanMapper;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.repository.loan.LoanRepaymentRepository;
import com.company.hrpayroll.repository.loan.LoanRepository;
import com.company.hrpayroll.service.loan.LoanService;
import com.company.hrpayroll.service.notification.NotificationRecipients;
import com.company.hrpayroll.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final EmployeeRepository employeeRepository;
    private final LoanMapper loanMapper;
    private final BusinessClock businessClock;
    private final NotificationService notificationService;
    private final NotificationRecipients recipients;

    private static final String TYPE_AGENT_REQUESTED = "AGENT_LOAN_REQUESTED";
    private static final String TYPE_AGENT_APPROVED = "AGENT_LOAN_APPROVED";
    private static final String TYPE_AGENT_REJECTED = "AGENT_LOAN_REJECTED";

    @Override
    public LoanResponse requestLoan(LoanRequest request) {
        return createLoan(request, null);
    }

    @Override
    public LoanResponse requestLoanForAgent(LoanRequest request, Long agentId) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.employeeId()));

        // An agent can only ask for a loan for staff they supply, and only for active staff.
        if (!agentId.equals(employee.getAgentId())) {
            throw new InvalidOperationException("You can only request a loan for an employee you supply");
        }
        if (!"ACTIVE".equals(employee.getEmploymentStatus())) {
            throw new InvalidOperationException("A loan can only be requested for an active employee");
        }

        LoanResponse response = createLoan(request, agentId);

        String message = "A loan of " + kes(request.principalAmount()) + " was requested for "
                + employee.getFullName() + ". Review and decide.";
        recipients.admins().forEach(admin ->
                notificationService.notifyInApp(admin.getId(), TYPE_AGENT_REQUESTED, message, response.id()));

        return response;
    }

    private LoanResponse createLoan(LoanRequest request, Long requestedByAgentId) {
        if (!employeeRepository.existsById(request.employeeId())) {
            throw new ResourceNotFoundException("Employee not found with id: " + request.employeeId());
        }

        Loan loan = loanMapper.toLoan(request);

        // The loan starts owing principal + interest, so an installment larger
        // than that could never be deducted as stated.
        if (loan.getInstallmentAmount().compareTo(loan.getOutstandingBalance()) > 0) {
            throw new InvalidOperationException(
                    "Installment amount cannot exceed the total repayable amount of " + loan.getOutstandingBalance());
        }

        loan.setRequestDate(businessClock.today());
        loan.setRequestedByAgentId(requestedByAgentId);
        Loan saved = loanRepository.save(loan);
        return toResponseWithEmployeeName(saved);
    }

    @Override
    public LoanResponse approveLoan(Long loanId, BigDecimal interestRate) {
        Loan loan = findOrThrow(loanId);
        if (!"PENDING".equals(loan.getStatus())) {
            throw new InvalidOperationException("Only pending loans can be approved");
        }

        // Employees never set their own interest: the approver fixes it here,
        // and the balance becomes the full amount owed (principal + flat interest).
        if (interestRate != null) {
            loan.setInterestRate(interestRate);
            loan.setOutstandingBalance(LoanMapper.totalRepayable(loan.getPrincipalAmount(), interestRate));
        }

        if (loan.getInstallmentAmount().compareTo(loan.getOutstandingBalance()) > 0) {
            throw new InvalidOperationException(
                    "Installment amount cannot exceed the total repayable amount of " + loan.getOutstandingBalance());
        }

        loan.setStatus("APPROVED");
        loan.setApprovalDate(businessClock.today());
        Loan saved = loanRepository.save(loan);
        settleAgentRequest(saved, true);
        return toResponseWithEmployeeName(saved);
    }

    @Override
    public LoanResponse rejectLoan(Long loanId) {
        Loan loan = findOrThrow(loanId);
        if (!"PENDING".equals(loan.getStatus())) {
            throw new InvalidOperationException("Only pending loans can be rejected");
        }
        loan.setStatus("REJECTED");
        Loan saved = loanRepository.save(loan);
        settleAgentRequest(saved, false);
        return toResponseWithEmployeeName(saved);
    }

    @Override
    public List<LoanResponse> getLoansForEmployee(Long employeeId) {
        return loanRepository.findByEmployeeId(employeeId).stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::toResponseWithEmployeeName)
                .toList();
    }

    @Override
    public List<LoanResponse> getLoansForAgent(Long agentId) {
        List<Long> employeeIds = employeeRepository.findByAgentId(agentId).stream()
                .map(Employee::getId)
                .toList();

        if (employeeIds.isEmpty()) {
            return List.of();
        }

        return loanRepository.findByEmployeeIdIn(employeeIds).stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::toResponseWithEmployeeName)
                .toList();
    }

    @Override
    public List<LoanResponse> getAllLoans(String status) {
        List<Loan> loans = (status == null || status.isBlank())
                ? loanRepository.findAll()
                : loanRepository.findByStatus(status.trim().toUpperCase());

        return loans.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::toResponseWithEmployeeName)
                .toList();
    }

    @Override
    public LoanResponse getLoanById(Long loanId) {
        return toResponseWithEmployeeName(findOrThrow(loanId));
    }

    @Override
    public List<LoanRepaymentResponse> getRepaymentsForLoan(Long loanId) {
        findOrThrow(loanId);
        return loanRepaymentRepository.findByLoanId(loanId).stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(loanMapper::toLoanRepaymentResponse)
                .toList();
    }

    @Override
    public List<LoanDeductionResult> getActiveDeductionsForEmployees(List<Long> employeeIds) {
        return loanRepository.findByEmployeeIdInAndStatus(employeeIds, "APPROVED").stream()
                // Never deduct more than what's actually still owed.
                .map(loan -> new LoanDeductionResult(
                        loan.getId(),
                        loan.getEmployeeId(),
                        loan.getInstallmentAmount().min(loan.getOutstandingBalance())
                ))
                .filter(result -> result.installmentAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    @Override
    public void applyDeduction(Long loanId, Long payrollRunId, BigDecimal amountPaid) {
        Loan loan = findOrThrow(loanId);

        BigDecimal newBalance = loan.getOutstandingBalance().subtract(amountPaid);
        loan.setOutstandingBalance(newBalance.max(BigDecimal.ZERO));
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus("CLOSED");
        }
        loanRepository.save(loan);

        LoanRepayment repayment = LoanRepayment.builder()
                .loanId(loanId)
                .payrollRunId(payrollRunId)
                .amountPaid(amountPaid)
                .build();
        loanRepaymentRepository.save(repayment);
    }

    // A loan an agent raised has been decided: clear the Admins' "review" notices and tell the agent how it ended.
    private void settleAgentRequest(Loan loan, boolean approved) {
        if (loan.getRequestedByAgentId() == null) {
            return;
        }

        notificationService.resolveByReference(TYPE_AGENT_REQUESTED, loan.getId());

        String employeeName = employeeRepository.findById(loan.getEmployeeId())
                .map(Employee::getFullName).orElse("your employee");
        String message = approved
                ? "The loan of " + kes(loan.getPrincipalAmount()) + " for " + employeeName
                        + " was approved. It will be deducted from their pay automatically."
                : "The loan of " + kes(loan.getPrincipalAmount()) + " for " + employeeName + " was rejected.";

        recipients.agentLogin(loan.getRequestedByAgentId()).ifPresent(agentUser ->
                notificationService.notifyInApp(
                        agentUser.getId(), approved ? TYPE_AGENT_APPROVED : TYPE_AGENT_REJECTED, message, loan.getId()));
    }

    private static String kes(BigDecimal amount) {
        return String.format(Locale.US, "KES %,.2f", amount);
    }

    private Loan findOrThrow(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + id));
    }

    private LoanResponse toResponseWithEmployeeName(Loan loan) {
        String employeeName = employeeRepository.findById(loan.getEmployeeId())
                .map(Employee::getFullName).orElse(null);
        return loanMapper.toLoanResponse(loan, employeeName);
    }
}