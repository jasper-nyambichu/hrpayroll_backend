// mapper/notification/NotificationMapper.java
package com.company.hrpayroll.mapper.notification;

import com.company.hrpayroll.dto.response.notification.NotificationResponse;
import com.company.hrpayroll.entity.notification.Notification;
import org.springframework.stereotype.Service;

@Service
public class NotificationMapper {

    public NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getChannel(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReferenceId()
        );
    }
}