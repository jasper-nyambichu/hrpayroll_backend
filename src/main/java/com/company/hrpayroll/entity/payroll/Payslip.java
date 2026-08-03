// entity/payroll/Payslip.java
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
@Table(name = "payslips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payslip extends BaseEntity {

    @Column(nullable = false)
    private Long payrollRunId;

    @Column(nullable = false)
    private Long employeeId;

    // Earnings
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal basicSalary;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal houseAllowance = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal transportAllowance = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal overtimePay = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal otherAllowances = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grossPay;

    // Statutory deductions
    @Column(precision = 15, scale = 2)
    private BigDecimal shifDeduction;

    @Column(precision = 15, scale = 2)
    private BigDecimal nssfDeduction;

    @Column(precision = 15, scale = 2)
    private BigDecimal housingLevyDeduction;

    @Column(precision = 15, scale = 2)
    private BigDecimal payeDeduction;

    @Column(precision = 15, scale = 2)
    private BigDecimal nitaDeduction;

    // Other deductions
    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal loanDeduction = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal netPay;
}