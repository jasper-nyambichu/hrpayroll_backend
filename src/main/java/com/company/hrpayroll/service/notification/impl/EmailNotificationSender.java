// service/notification/impl/EmailNotificationSender.java
package com.company.hrpayroll.service.notification.impl;

import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.service.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Override
    public String channel() {
        return "EMAIL";
    }

    @Override
    public void send(Long recipientUserId, String message) {
        userRepository.findById(recipientUserId).ifPresentOrElse(user -> {
            // User entity has no email field directly today (that lives on
            // Employee/Agent) — resolving the actual address is left as a
            // lookup against employee/agent once those relations are needed
            // here. For now this logs intent, which is enough to prove the
            // dispatch pipeline works end to end.
            log.info("Would send EMAIL to user {}: {}", user.getUsername(), message);
        }, () -> log.warn("Cannot send email — user {} not found", recipientUserId));
    }
}