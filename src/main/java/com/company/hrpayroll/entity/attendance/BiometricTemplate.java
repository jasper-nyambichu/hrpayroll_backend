// entity/attendance/BiometricTemplate.java — corrected
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
@Table(name = "biometric_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricTemplate extends BaseEntity {

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Long branchId;

    // Fixed: removed @Lob. Fingerprint templates are small (bytes to a few
    // KB), not large-object territory — a plain byte[] maps to PostgreSQL's
    // bytea type by default, which matches the Flyway migration exactly.
    // @Lob would have forced PostgreSQL's oid (large object) mechanism
    // instead, which is what caused the schema validation mismatch.
    @Column(nullable = false)
    private byte[] templateData;

    @Column(nullable = false)
    private Integer fingerIndex; // 1 = primary, 2 = backup finger

    private Double qualityScore;

    @Column(nullable = false)
    private LocalDateTime enrolledAt;
}