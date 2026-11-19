// entity/settlement/AgentSettlement.java
package com.company.hrpayroll.entity.settlement;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// What one agent is paid for one period. The agent pays their own employees from this amount:
// salaries (monthly salary / days in month x days paid) + PAYE + NSSF + NITA + agent fee + VAT
//   - loans - damages - advances
// Runs made before 2026-11-19 paid days worked x one daily rate instead (payBasis DAILY_RATE); their
// figures are kept and shown as they were.
@Entity
@Table(name = "agent_settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSettlement extends BaseEntity {

    public static final String BASIS_SALARY = "SALARY";
    public static final String BASIS_DAILY_RATE = "DAILY_RATE";

    @Column(nullable = false)
    private Long agentId;

    // SALARY (each employee's own salary) or DAILY_RATE (older runs: days x one company rate).
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String payBasis = BASIS_SALARY;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private Integer employeeCount;

    @Column(nullable = false)
    private Integer daysWorkedTotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal daysPay;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal payeTotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal nssfTotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal nitaTotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal agentFee;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRateUsed;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal vatAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal loanTotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal damageTotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal advanceRecovered;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPayable;

    // The approved request the Admin releases to send this money. Null when nothing is payable.
    private Long paymentRequestId;

    private Long calculatedByUserId;
}
