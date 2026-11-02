// event/listener/NotificationEventListener.java
package com.company.hrpayroll.event;

import com.company.hrpayroll.event.LeaveApprovedEvent;
import com.company.hrpayroll.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

// LeaveServiceImpl never calls NotificationService directly — it publishes an event via
// ApplicationEventPublisher and this listener reacts.
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

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