// entity/agent/Agent.java
package com.company.hrpayroll.entity.agent;

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
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    private String contactPerson;
    private String phone;
    private String email;
    private String address;

    private String bankName;
    private String bankAccountNumber;

    @Column(length = 20)
    private String kraPin;

    private LocalDate contractStartDate;
    private LocalDate contractEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChargeType chargeType;

    // Used when chargeType = PER_EMPLOYEE or FIXED_MONTHLY or CUSTOM
    @Column(precision = 15, scale = 2)
    private BigDecimal chargeAmount;

    // Used when chargeType = PERCENTAGE_OF_GROSS (e.g. 10.00 for 10%)
    @Column(precision = 5, scale = 2)
    private BigDecimal chargePercentage;

    // Turns VAT on/off for this agent in the payroll formula.
    @Builder.Default
    @Column(nullable = false)
    private boolean vatRegistered = false;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}