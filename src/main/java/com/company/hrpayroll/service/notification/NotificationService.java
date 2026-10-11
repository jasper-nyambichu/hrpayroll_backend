// service/notification/NotificationService.java
package com.company.hrpayroll.service.notification;

import com.company.hrpayroll.dto.response.notification.NotificationResponse;

import java.util.List;

public interface NotificationService {
    // The one method the rest of the system actually calls — triggered from
    // domain events, not called directly by unrelated modules (see event/
    // listeners below).
    void notify(Long recipientUserId, String type, String message, List<String> channels);

    // In-app only, with a type and the record it is about, so the bell can link to
    // it and it can be cleared later (see resolveByReference).
    void notifyInApp(Long recipientUserId, String type, String message, Long referenceId);

    // Marks every unread notification of this type about this record as read, for
    // every recipient. Used when the thing they were notified about is decided.
    void resolveByReference(String type, Long referenceId);

    List<NotificationResponse> getNotificationsForUser(Long userId);

    // Scoped to the caller: a user can only mark their own notifications.
    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);

    long getUnreadCount(Long userId);
}