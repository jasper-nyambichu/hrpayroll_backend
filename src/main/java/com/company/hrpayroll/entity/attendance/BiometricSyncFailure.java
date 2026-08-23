// entity/attendance/BiometricSyncFailure.java
package com.company.hrpayroll.entity.attendance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_sync_failures")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricSyncFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private String eventId;
    private String deviceSerial;
    private Long employeeId;

    @Column(nullable = false)
    private String reason;
}