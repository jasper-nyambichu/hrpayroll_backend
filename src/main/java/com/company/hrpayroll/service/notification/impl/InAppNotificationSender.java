// service/notification/impl/InAppNotificationSender.java
package com.company.hrpayroll.service.notification.impl;

import com.company.hrpayroll.entity.notification.Notification;
import com.company.hrpayroll.repository.notification.NotificationRepository;
import com.company.hrpayroll.service.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InAppNotificationSender implements NotificationSender {

    private final NotificationRepository notificationRepository;

    @Override
    public String channel() {
        return "IN_APP";
    }

    @Override
    public void send(Long recipientUserId, String message) {
        Notification notification = Notification.builder()
                .recipientUserId(recipientUserId)
                .channel("IN_APP")
                .type("GENERAL")
                .message(message)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }
}