// entity/payroll/PayrollRun.java
package com.company.hrpayroll.entity.payroll;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payroll_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRun extends BaseEntity {

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    // Comma-separated IDs describing what was selected when the run started
    // (branch IDs, agent IDs, department IDs) — kept for audit purposes.
    private String scopeBranchIds;
    private String scopeAgentIds;
    private String scopeDepartmentIds;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private PayrollRunStatus status = PayrollRunStatus.IN_PROGRESS;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalGrossPayroll;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalNetPayroll;

    private Long initiatedByUserId;
}