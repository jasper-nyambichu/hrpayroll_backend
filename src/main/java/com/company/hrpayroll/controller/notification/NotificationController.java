// controller/notification/NotificationController.java
package com.company.hrpayroll.controller.notification;

import com.company.hrpayroll.dto.response.ApiResponse;
import com.company.hrpayroll.dto.response.notification.NotificationResponse;
import com.company.hrpayroll.security.AccessGuard;
import com.company.hrpayroll.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Deliberately no @PreAuthorize role checks here beyond "authenticated" —
// every endpoint operates on the CALLER's own notifications only, resolved
// via AccessGuard.currentUserId(), so there is no cross-user data to
// accidentally expose. Any authenticated role — staff, agent, or employee
// — can check their own notifications.
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AccessGuard accessGuard;

    // The caller's 50 most recent notifications, newest first.
    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMyNotifications() {
        return ApiResponse.success(notificationService.getNotificationsForUser(accessGuard.currentUserId()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        return ApiResponse.success(notificationService.getUnreadCount(accessGuard.currentUserId()));
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead(accessGuard.currentUserId());
        return ApiResponse.success("All marked as read", null);
    }

    // Scoped to the caller — marking someone else's notification is a 404.
    @PutMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(accessGuard.currentUserId(), notificationId);
        return ApiResponse.success("Marked as read", null);
    }
}