package com.company.hrpayroll.entity.auth;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// The sign-in finger of a Branch Manager or Back Office login (not of an employee): it opens that login on its
// branch's PC and is never attendance. Only FingerprintCipher reads the template.
@Entity
@Table(name = "login_fingerprints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginFingerprint extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "template_encrypted", nullable = false)
    private byte[] templateEncrypted;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @Column(name = "enrolled_device_id")
    private Long enrolledDeviceId;
}
