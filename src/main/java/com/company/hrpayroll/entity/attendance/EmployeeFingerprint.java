package com.company.hrpayroll.entity.attendance;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// The company-wide copy of one enrolled finger. It belongs to the employee rather than to a terminal,
// so it stays valid when they move to another branch. Only FingerprintCipher reads the template.
@Entity
@Table(name = "employee_fingerprints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeFingerprint extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "finger_slot", nullable = false)
    private Short fingerSlot;

    @Column(name = "template_encrypted", nullable = false)
    private byte[] templateEncrypted;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @Column(name = "enrolled_device_id")
    private Long enrolledDeviceId;
}
