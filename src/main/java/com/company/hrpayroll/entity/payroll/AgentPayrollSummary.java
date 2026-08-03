// entity/payroll/AgentPayrollSummary.java
package com.company.hrpayroll.entity.payroll;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "agent_payroll_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentPayrollSummary extends BaseEntity {

    @Column(nullable = false)
    private Long payrollRunId;

    @Column(nullable = false)
    private Long agentId;

    private Integer employeeCount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grossSalaries;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal serviceCharge;

    @Column(precision = 15, scale = 2)
    private BigDecimal shifTotal;

    @Column(precision = 15, scale = 2)
    private BigDecimal nssfTotal;

    @Column(precision = 15, scale = 2)
    private BigDecimal housingLevyTotal;

    @Column(precision = 15, scale = 2)
    private BigDecimal payeTotal;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal employeeLoanDeductionsTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal advanceRecoveryTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalBeforeVat;

    @Builder.Default
    @Column(nullable = false)
    private boolean vatApplied = false;

    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal vatRateUsed = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal finalAmountPayable;
}