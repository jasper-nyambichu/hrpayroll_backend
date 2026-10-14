// event/listener/NotificationEventListener.java
package com.company.hrpayroll.event;

import com.company.hrpayroll.event.LeaveApprovedEvent;
import com.company.hrpayroll.event.PayrollCompletedEvent;
import com.company.hrpayroll.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

// This is the payoff of the event-based design: PayrollRunServiceImpl and
// LeaveServiceImpl never call NotificationService directly — they publish
// an event via ApplicationEventPublisher and this listener reacts. Payroll
// stays fully decoupled from *how* people get notified.
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    // @Async here is exactly the virtual-thread pattern discussed earlier —
    // notifying potentially hundreds of employees after a payroll run is
    // pure I/O wait, so it runs on the virtual-thread executor and never
    // blocks the payroll transaction that triggered it.
    //
    // AFTER_COMMIT: the event is published inside the payroll transaction, but
    // nobody must be told a payslip is available for a run that then rolled back.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPayrollCompleted(PayrollCompletedEvent event) {
        for (Long userId : event.employeeUserIds()) {
            notificationService.notify(
                    userId, "PAYROLL_COMPLETED",
                    "Your payslip for payroll run #" + event.payrollRunId() + " is now available.",
                    List.of("EMAIL", "IN_APP")
            );
        }
    }

    @Async
    @EventListener
    public void onLeaveApproved(LeaveApprovedEvent event) {
        notificationService.notify(
                event.employeeUserId(), "LEAVE_APPROVED",
                "Your leave request has been approved.",
                List.of("EMAIL", "IN_APP")
        );
    }
}