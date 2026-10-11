// repository/notification/NotificationRepository.java
package com.company.hrpayroll.repository.notification;

import com.company.hrpayroll.entity.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdAndReadFalse(Long recipientUserId);

    List<Notification> findByRecipientUserId(Long recipientUserId);

    // The bell shows the latest 50, newest first.
    List<Notification> findTop50ByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    // Every unread notification about one record, for every recipient.
    List<Notification> findByTypeAndReferenceIdAndReadFalse(String type, Long referenceId);

    long countByRecipientUserIdAndReadFalse(Long recipientUserId);
}