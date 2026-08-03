// entity/audit/AuditLog.java
package com.company.hrpayroll.entity.audit;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    private Long performedByUserId;

    @Column(nullable = false, length = 100)
    private String action; // e.g. "PAYROLL_RUN_EXECUTED", "EMPLOYEE_UPDATED"

    @Column(nullable = false, length = 50)
    private String entityType;

    private Long entityId;

    @Lob
    private String detailsJson; // Before/after snapshot, kept as JSON text
}