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
// (days worked x daily rate) + PAYE + NSSF + NITA + VAT + agent fee - loans - damages - advances
@Entity
@Table(name = "agent_settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSettlement extends BaseEntity {

    @Column(nullable = false)
    private Long agentId;

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
