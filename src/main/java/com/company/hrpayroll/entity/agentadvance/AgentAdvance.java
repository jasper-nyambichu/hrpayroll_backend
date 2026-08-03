// entity/agentadvance/AgentAdvance.java
package com.company.hrpayroll.entity.agentadvance;

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
@Table(name = "agent_advances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentAdvance extends BaseEntity {

    @Column(nullable = false)
    private Long agentId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate advanceDate;

    private String purpose;

    // Starts equal to `amount`; reduced as it is recovered during payroll runs.
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "OUTSTANDING"; // OUTSTANDING, PARTIALLY_RECOVERED, FULLY_RECOVERED
}