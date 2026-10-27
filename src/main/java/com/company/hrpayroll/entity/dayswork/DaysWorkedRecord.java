// entity/dayswork/DaysWorkedRecord.java
package com.company.hrpayroll.entity.dayswork;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// The locked count of days one employee reported to work in a pay period. A row exists only once
// the Back Office has reviewed and locked the period, so payroll never reads a figure that is
// still changing. systemDays is what the attendance records said; daysWorked is the final figure
// (they differ only when the officer adjusted it, with a reason).
@Entity
@Table(name = "days_worked_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_days_worked_employee_period",
                columnNames = {"employee_id", "period_start", "period_end"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DaysWorkedRecord extends BaseEntity {

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private int systemDays;

    @Column(nullable = false)
    private int daysWorked;

    @Builder.Default
    @Column(nullable = false)
    private boolean adjusted = false;

    @Column(length = 500)
    private String adjustmentReason;

    @Column(nullable = false)
    private Long lockedByUserId;

    @Column(nullable = false)
    private LocalDateTime lockedAt;
}
