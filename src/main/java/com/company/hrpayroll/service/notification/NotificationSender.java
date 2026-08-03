// service/notification/NotificationSender.java
package com.company.hrpayroll.service.notification;

// One interface, multiple implementations — EmailSender, SmsSender,
// InAppSender. NotificationService picks the right one per channel rather
// than every caller knowing about email/SMS/in-app details directly.
public interface NotificationSender {
    String channel(); // "EMAIL", "SMS", "IN_APP"
    void send(Long recipientUserId, String message);
}