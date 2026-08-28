// entity/attendance/BiometricDeviceMapping.java
package com.company.hrpayroll.entity.attendance;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Replaces BiometricTemplate. The ZKTeco terminal owns fingerprint capture
// and matching entirely on-device — the backend never sees raw template
// bytes. All we store is the translation between the terminal's internal
// user id and our real employee id, established once at enrollment time.
@Entity
@Table(name = "biometric_device_mappings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_device_terminal_user",
                columnNames = {"device_id", "terminal_user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricDeviceMapping extends BaseEntity {

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    // The numeric PIN/user-id the ZKTeco terminal assigns internally when
    // an employee enrolls directly at the device.
    @Column(name = "terminal_user_id", nullable = false, length = 20)
    private String terminalUserId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private LocalDateTime enrolledAt;
}