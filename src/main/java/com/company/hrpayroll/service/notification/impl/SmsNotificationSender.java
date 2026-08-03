// service/notification/impl/SmsNotificationSender.java
package com.company.hrpayroll.service.notification.impl;

import com.company.hrpayroll.service.notification.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsNotificationSender implements NotificationSender {

    @Override
    public String channel() {
        return "SMS";
    }

    @Override
    public void send(Long recipientUserId, String message) {
        // Placeholder until an SMS gateway (e.g. Africa's Talking) is
        // chosen and its API key is available — same interface either way.
        log.info("Would send SMS to user {}: {}", recipientUserId, message);
    }
}