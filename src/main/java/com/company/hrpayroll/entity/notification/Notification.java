// entity/notification/Notification.java
package com.company.hrpayroll.entity.notification;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Column(nullable = false)
    private Long recipientUserId;

    @Column(nullable = false, length = 30)
    private String channel; // EMAIL, SMS, IN_APP

    @Column(nullable = false, length = 50)
    private String type; // LEAVE_APPROVED, PAYROLL_COMPLETED, EMPLOYEE_REQUEST_SUBMITTED, ...

    @Column(nullable = false)
    private String message;

    // The record this notification is about (e.g. an employee_creation_requests id).
    // Null for notifications that are not about a specific record.
    private Long referenceId;

    @Builder.Default
    @Column(nullable = false)
    private boolean read = false;
}