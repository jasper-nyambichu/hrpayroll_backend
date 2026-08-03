// service/notification/impl/NotificationServiceImpl.java
package com.company.hrpayroll.service.notification.impl;

import com.company.hrpayroll.dto.response.notification.NotificationResponse;
import com.company.hrpayroll.entity.notification.Notification;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.notification.NotificationMapper;
import com.company.hrpayroll.repository.notification.NotificationRepository;
import com.company.hrpayroll.service.notification.NotificationSender;
import com.company.hrpayroll.service.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final Map<String, NotificationSender> sendersByChannel;
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    // Spring injects all NotificationSender beans as a List; indexing them
    // by channel() here means adding a fourth channel later needs zero
    // changes to this class — just a new @Component implementing the interface.
    public NotificationServiceImpl(
            List<NotificationSender> senders,
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper
    ) {
        this.sendersByChannel = senders.stream()
                .collect(Collectors.toMap(NotificationSender::channel, Function.identity()));
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    @Override
    public void notify(Long recipientUserId, String type, String message, List<String> channels) {
        for (String channel : channels) {
            NotificationSender sender = sendersByChannel.get(channel);
            if (sender == null) {
                log.warn("No sender registered for channel '{}' — skipping", channel);
                continue;
            }
            sender.send(recipientUserId, message);
        }
    }

    @Override
    public List<NotificationResponse> getNotificationsForUser(Long userId) {
        return notificationRepository.findByRecipientUserId(userId).stream()
                .map(notificationMapper::toNotificationResponse)
                .toList();
    }

    @Override
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientUserIdAndReadFalse(userId);
    }
}