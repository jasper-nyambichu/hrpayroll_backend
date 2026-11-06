// entity/attendance/BiometricDevice.java
package com.company.hrpayroll.entity.attendance;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricDevice extends BaseEntity {

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false, unique = true, length = 100)
    private String deviceSerial;

    // Hashed device token used to authenticate the bridge-agent, separate
    // from any user's JWT.
    @Column(nullable = false)
    private String deviceTokenHash;

    private LocalDateTime lastSyncAt;

    // The branch PC's last report (every minute, and at once when the reader is plugged in or out).
    private LocalDateTime lastSeenAt;

    // Whether the fingerprint reader was plugged in at that report. Null until the PC first reports.
    private Boolean readerConnected;

    // When readerConnected last changed, so the web can say how long a reader has been unplugged.
    private LocalDateTime readerChangedAt;

    @Column(length = 30)
    private String appVersion;

    // Scans saved on that PC and not yet received, at that report.
    private Integer pendingScans;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}