// service/notification/NotificationService.java
package com.company.hrpayroll.service.notification;

import com.company.hrpayroll.dto.response.notification.NotificationResponse;

import java.util.List;

public interface NotificationService {
    // The one method the rest of the system actually calls — triggered from
    // domain events, not called directly by unrelated modules (see event/
    // listeners below).
    void notify(Long recipientUserId, String type, String message, List<String> channels);

    List<NotificationResponse> getNotificationsForUser(Long userId);
    void markAsRead(Long notificationId);
    long getUnreadCount(Long userId);
}