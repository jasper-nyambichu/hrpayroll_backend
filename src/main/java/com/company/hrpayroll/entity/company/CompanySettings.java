// entity/company/CompanySettings.java
package com.company.hrpayroll.entity.company;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "company_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanySettings extends BaseEntity {

    @Column(nullable = false)
    private Long companyId;

    // The rate used for every VAT-registered agent (e.g. 16.00 for 16%).
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Builder.Default
    private Double defaultWorkingHoursPerDay = 8.0;

    @Builder.Default
    private Double overtimeMultiplier = 1.5;

    @Builder.Default
    @Column(length = 20)
    private String weekendDays = "SATURDAY,SUNDAY";

    @Builder.Default
    @Column(length = 20)
    private String payrollPeriod = "MONTHLY";

    // ---- Pay model: days worked x daily rate, with SHIF and NSSF added on top (Admin-set) ----

    // What one day worked pays (KES). 30 days x 1,000 = 30,000.
    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyRate = new BigDecimal("1000.00");

    // PERCENT: shifValue is a percentage of pay. FIXED: shifValue is a flat amount per payslip.
    @Builder.Default
    @Column(nullable = false, length = 10)
    private String shifMode = "PERCENT";

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal shifValue = new BigDecimal("2.75");

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String nssfMode = "PERCENT";

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal nssfValue = new BigDecimal("6.00");

    // The owner's formula leaves these out, so they are off until an Admin decides otherwise.
    @Builder.Default
    @Column(nullable = false)
    private boolean applyHousingLevy = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean applyPaye = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean applyNita = false;

    // The most that loan instalments, damages and any tax/levy may take off one payslip, as a
    // percentage of gross pay. Kenyan law limits wage deductions to two thirds of wages.
    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal deductionCeilingPercent = new BigDecimal("66.67");
}