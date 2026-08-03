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
}
