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

    // Days x rate: how many days the Back Office locked for the period, and what a day paid.
    @Builder.Default
    @Column(nullable = false)
    private int daysWorked = 0;

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyRate = BigDecimal.ZERO;

    // Earnings. basicSalary is the pay for the days worked (days x daily rate).
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

    // Already taken out of grossPay (approved leave of a type with paid = false).
    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal unpaidLeaveDeduction = BigDecimal.ZERO;

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

    // Damages the employee agreed to (or that were overridden), taken out of net pay.
    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal damageDeduction = BigDecimal.ZERO;

    // Gross pay + SHIF + NSSF: the figure the owner calls the total employee salary. SHIF and NSSF
    // are added on top of the pay rather than taken out of it.
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalEmployeeSalary;

    // Employer-borne NITA levy — recorded for remittance, NOT deducted from netPay.
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