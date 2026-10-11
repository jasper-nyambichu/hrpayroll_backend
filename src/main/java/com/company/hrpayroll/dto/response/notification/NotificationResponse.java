// dto/response/notification/NotificationResponse.java
package com.company.hrpayroll.dto.response.notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String channel,
        String type,
        String message,
        boolean read,
        LocalDateTime createdAt,
        Long referenceId
) {}