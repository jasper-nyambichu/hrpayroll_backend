// entity/damage/Damage.java
package com.company.hrpayroll.entity.damage;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// A damage recorded against an employee by the Back Office. It only becomes deductible once the
// employee has agreed: by fingerprint at the clock-in device, or by an officer's override with a
// mandatory reason. An override is flagged permanently (overridden = true), even after the damage
// has been deducted, so it can always be told apart from a consented one.
@Entity
@Table(name = "damages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Damage extends BaseEntity {

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDate damageDate;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PENDING_CONSENT"; // PENDING_CONSENT, APPROVED, CANCELLED, DEDUCTED

    @Column(nullable = false)
    private Long recordedByUserId;

    @Column(length = 20)
    private String approvalMethod; // FINGERPRINT, OVERRIDE

    private LocalDateTime consentAt;
    private Long consentDeviceId;

    // Event id of the device's consent scan, so a retried consent is recognised (idempotency).
    @Column(unique = true, length = 100)
    private String consentEventId;

    @Builder.Default
    @Column(nullable = false)
    private boolean overridden = false;

    @Column(length = 500)
    private String overrideReason;

    private Long overriddenByUserId;
    private LocalDateTime overriddenAt;

    // Set when a payroll run deducts the damage.
    private Long payrollRunId;
    private LocalDateTime deductedAt;
}
