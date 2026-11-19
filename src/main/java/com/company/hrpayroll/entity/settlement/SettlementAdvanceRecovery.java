package com.company.hrpayroll.entity.settlement;

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

// How much one payment run took back from one of the agent's advances, and what was left owing after it.
@Entity
@Table(name = "settlement_advance_recoveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementAdvanceRecovery extends BaseEntity {

    @Column(nullable = false)
    private Long settlementId;

    @Column(nullable = false)
    private Long advanceId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;
}
